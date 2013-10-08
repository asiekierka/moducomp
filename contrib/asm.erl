% Areia-1 CPU assembler
% by Ben "GreaseMonkey" Russell, 2013
% Licence: CC0: http://creativecommons.org/publicdomain/zero/1.0/
%
% NOTES:
% - Expressions do not follow proper precedence yet (e.g. a-b-c ends up as a-(b-c))
% - Most of the time, if you get a syntax error, you'll just get an erlang badmatch.
-module(asm).
-export([main/0, main/1]).
-export([parse_asm_string/1, parse_asm_file/1]).
-export([mem_dump/1]).
-export([tok_str/1]).
-include("asm.hrl").

-define(IS_LETTER(C), ((C >= $a andalso C =< $z) orelse (C >= $A andalso C =< $Z))).
-define(IS_DIGIT(C), (C >= $0 andalso C =< $9)).
-define(IS_WS(C), (C == $  orelse C == $\t)).
-define(IS_W_B(C), (C == $b orelse C == $B orelse C == $w orelse C == $W)).

%
% Operation dictionary
%
opsize($b) -> 0;
opsize($B) -> 0;
opsize($w) -> 1;
opsize($W) -> 1.

op0("nop")   -> 0;
op0("ret")   -> 1;
op0("popf")  -> 2;
op0("pushf") -> 3;
op0("cli")   -> 4;
op0("sei")   -> 5;
op0("hlt")   -> 6;
% unused op 7
op0("ss0")   -> 8;
op0("ss1")   -> 9;
op0("ss2")   -> 10;
op0("ss3")   -> 11;
% dbg not supported until it has proper semantics
op0(_) -> nil.

op1("move") -> 0;
op1("add")  -> 1;
op1("cmp")  -> 2;
op1("sub")  -> 3;
op1("xor")  -> 4;
op1("or")   -> 5;
op1("and")  -> 6;
% unused op 7
% unused op 8
op1("asl") -> 9; op1("lsl") -> 9;
op1("asr") -> 10;
op1("lsr") -> 11;
op1("rol") -> 12;
op1("ror") -> 13;
op1("rcl") -> 14;
op1("rcr") -> 15;
op1(_) -> nil.

op2("ld") -> 0;
op2("st") -> 1;
op2(_) -> nil.

op3("jz")  -> 0;
op3("jnz") -> 1;
op3("je")  -> 0;
op3("jne") -> 1;
op3("jc")  -> 2;
op3("jnc") -> 3;
op3("jv")  -> 4;
op3("jnv") -> 5;
op3("js")  -> 6;
op3("jns") -> 7;
op3("jmp") -> 8;
op3("jsr") -> 9;
op3(_) -> nil.

optype(X, nil, nil, nil) when X /= nil -> {op0, X};
optype(nil, X, nil, nil) when X /= nil -> {op1, X};
optype(nil, nil, X, nil) when X /= nil -> {op2, X};
optype(nil, nil, nil, X) when X /= nil -> {op3, X};
optype(_, _, _, _) -> error.

optype(X) -> optype(op0(X), op1(X), op2(X), op3(X)).

%
% Calculation tree
%

label_add(State, Name) ->
	false = dict:is_key(Name, State#asm_state.labels),

	State#asm_state {
		labels = dict:store(Name, State#asm_state.pos, State#asm_state.labels)
	}.

calc_unop({unop, o_low, V}) -> (V band 16#FF);
calc_unop({unop, o_high, V}) -> ((V bsr 8) band 16#FF);
calc_unop({unop, o_top4, V}) -> ((V bsr 16) band 16#F);
calc_unop({unop, o_top8, V}) -> ((V bsr 12) band 16#FF);
calc_unop({unop, o_neg, V}) -> -V.

calc_binop({binop, o_add, V1, V2}) -> V1 + V2;
calc_binop({binop, o_sub, V1, V2}) -> V1 - V2;
calc_binop({binop, o_mul, V1, V2}) -> V1 * V2;
calc_binop({binop, o_asl, V1, V2}) -> V1 bsl V2;
calc_binop({binop, o_asr, V1, V2}) -> V1 bsr V2;
calc_binop({binop, o_and, V1, V2}) -> V1 band V2;
calc_binop({binop, o_xor, V1, V2}) -> V1 bxor V2;
calc_binop({binop, o_or, V1, V2}) -> V1 bor V2.

calc(_State, N, _AtEnd) when is_integer(N) -> N;
calc(State, {label, Name}, AtEnd) ->
	case dict:is_key(Name, State#asm_state.labels) of
		true -> dict:fetch(Name, State#asm_state.labels);
		false -> case AtEnd of
			true -> error(lists:flatten(io_lib:format("label ~p undefined~n", [Name])));
			false -> {label, Name}
		end
	end;
calc(State, {binop, Op, V1, V2}, AtEnd) ->
	N1 = calc(State, V1, AtEnd),
	N2 = calc(State, V2, AtEnd),
	case {is_integer(N1), is_integer(N2)} of
		{true, true} -> calc_binop({binop, Op, N1, N2});
		_ -> {binop, Op, N1, N2}
	end;
calc(State, {unop, Op, V}, AtEnd) ->
	N = calc(State, V, AtEnd),
	case is_integer(N) of
		true -> calc_unop({unop, Op, N});
		_ -> {unop, Op, N}
	end;
calc(State, {shield, C}, AtEnd) ->
	calc(State, C, AtEnd);
calc(State, {calc, C}, AtEnd) ->
	calc(State, C, AtEnd).

%
% Memory access
%

mem_dump(State) ->
	Org = State#asm_state.org,
	Eof = State#asm_state.eof,
	Mem = State#asm_state.mem,

	Data = lists:reverse(lists:foldl(fun (I, Acc) ->
		[calc(State, array:get(I, Mem), true)|Acc]
	end, [], lists:seq(Org, Eof-1))),
	{Org, Data}.

% mem_seek(State, Addr) ->
% 	State#asm_state{pos = Addr}.

mem_write(State, []) -> State;
mem_write(State, [H|T]) ->
	M1 = case State#asm_state.mem of
		nil -> array:new([{default, 16#FF}]);
		Mx -> Mx
	end,

	P1 = State#asm_state.pos,
	%io:format("write ~p ~p~n", [P1, H]),
	M2 = array:set(P1, calc(State, H, false), M1),
	P2 = (P1 + 1),

	O1 = case State#asm_state.org of
		nil -> P1;
		_ -> State#asm_state.org
	end,
	E1 = case State#asm_state.eof of
		nil -> P2;
		_ -> case (P2 > State#asm_state.eof) of
			true -> P2;
			false -> State#asm_state.eof
		end
	end,

	State2 = State#asm_state{
		mem = M2,
		pos = P2,
		org = O1,
		eof = E1
	},

	mem_write(State2, T).

mem_write_imm_size(State, 1, Imm) ->
	mem_write(State, [
		{calc, {unop, o_low, Imm}}]);
mem_write_imm_size(State, 2, Imm) ->
	mem_write(State, [
		{calc, {unop, o_low, Imm}},
		{calc, {unop, o_high, Imm}}]).

%
% Tokenisation functions
%

ensure_pow2(N) ->
	A0 = N,
	A1 = A0 bor (A0 bsr 16),
	A2 = A1 bor (A1 bsr 8),
	A3 = A2 bor (A2 bsr 4),
	A4 = A3 bor (A3 bsr 2),
	A5 = A4 bor (A4 bsr 1),

	A5 = N*2-1.

skip_ws([C|T]) when ?IS_WS(C) -> skip_ws(T);
skip_ws(L) -> L.

tok_str([$"|T], Acc) -> {ok, lists:reverse(Acc), T};
tok_str([$\\, $\\ | T], Acc) -> tok_str(T, [$\\ | Acc]);
tok_str([$\\, $n | T], Acc) -> tok_str(T, [$\n | Acc]);
tok_str([$\\, $r | T], Acc) -> tok_str(T, [$\r | Acc]);
tok_str([$\\, $t | T], Acc) -> tok_str(T, [$\t | Acc]);
tok_str([$\\, $b | T], Acc) -> tok_str(T, [$\b | Acc]);
tok_str([$\\, $" | T], Acc) -> tok_str(T, [$" | Acc]);
tok_str([$\\, $x, D0, D1 | T], Acc) ->
	{ok, N, L2} = tok_int_hex([D0, D1], 0),
	tok_str(L2 ++ T, [(N band 16#FF) | Acc]);
tok_str([C|T], Acc) -> tok_str(T, [C | Acc]).

tok_str([C|T]) when C == $" -> tok_str(T, []);
tok_str(_) -> error.

tok_int_bin([C|T], N) when C >= $0 andalso C =< $1 -> tok_int_bin(T, N*2 + (C-$0));
tok_int_bin(L, N) -> {ok, N, L}.

tok_int_dec([C|T], N) when C >= $0 andalso C =< $9 -> tok_int_dec(T, N*10 + (C-$0));
tok_int_dec(L, N) -> {ok, N, L}.

tok_int_hex([C|T], N) when C >= $0 andalso C =< $9 -> tok_int_hex(T, N*16 + (C-$0));
tok_int_hex([C|T], N) when C >= $a andalso C =< $f -> tok_int_hex(T, N*16 + (C-$a+10));
tok_int_hex([C|T], N) when C >= $A andalso C =< $F -> tok_int_hex(T, N*16 + (C-$A+10));
tok_int_hex(L, N) -> {ok, N, L}.

tok_int([$-|T]) -> case tok_int(T) of
		{ok, N, L} -> {ok, -N, L};
		error -> error
	end;
tok_int([$$|T]) -> tok_int_hex(T, 0);
tok_int([$%|T]) -> tok_int_bin(T, 0);
tok_int(L = [C|_]) when ?IS_DIGIT(C) -> tok_int_dec(L, 0).

tok_name([C|T], Acc) when ?IS_LETTER(C) orelse C == $_ orelse ?IS_DIGIT(C) -> tok_name(T, [C|Acc]);
tok_name(L, Acc) -> {ok, lists:reverse(Acc), L}.

tok_name(L = [C|_]) when ?IS_LETTER(C) orelse C == $_ -> tok_name(L, []).

tok_num_lit(L = [C|_]) when ?IS_LETTER(C) orelse C == $_ ->
	{ok, Name, L1} = tok_name(L),
	{ok, {calc, {label, Name}}, L1};
tok_num_lit(L) -> tok_int(L).

% TODO: apply a shunting yard rather than use RTL precedence.
tok_num([$( | T]) ->
	{ok, Tok, L1} = tok_num(T),
	[$) | L2] = skip_ws(L1),
	{ok, {calc, {shield, Tok}}, L2};
tok_num(L) ->
	{ok, Tok, L1} = tok_num_lit(L),
	L2 = skip_ws(L1),
	case L2 of
		[Cx|Tx] when Cx == $+; Cx == $-; Cx == $*; Cx == $|; Cx == $&; Cx == $^ ->
			Op = case Cx of
				$+ -> o_add;
				$- -> o_sub;
				$* -> o_mul;
				$| -> o_or;
				$& -> o_and;
				$^ -> o_xor
			end,
			{ok, Tok2, Lx1} = tok_num(skip_ws(Tx)),
			{ok, {calc, {binop, Op, Tok, Tok2}}, Lx1};
		[Cx,Cx|Tx] when Cx == $<; Cx == $> ->
			Op = case Cx of
				$< -> o_and;
				$> -> o_xor
			end,
			{ok, Tok2, Lx1} = tok_num(skip_ws(Tx)),
			{ok, {calc, {binop, Op, Tok, Tok2}}, Lx1};
		_ -> {ok, Tok, L2}
	end.

tok_reg_inrange(error) -> error;
tok_reg_inrange(R = {ok, X, _}) when X >= 0 andalso X =< 15 -> R.

tok_reg([$@|T]) -> tok_reg_inrange(tok_num(T));
tok_reg(_) -> error.

parse_end([]) -> ok;
parse_end([$; | _]) -> ok.

%
% Directive parsing functions
%
parse_direc("dd", L, State) ->
	{ok, N, L2} = tok_num(L),
	State2 = mem_write(State, [
		{calc, {unop, o_low, N}},
		{calc, {unop, o_high, N}},
		{calc, {unop, o_low, {binop, o_asr, N, 16}}},
		{calc, {unop, o_high, {binop, o_asr, N, 16}}}]),
	L3 = skip_ws(L2),
	case L3 of
		[$,|Lx2] ->
			parse_direc("dd", skip_ws(Lx2), State2);
		_ ->
			parse_end(L3),
			State2
	end;

parse_direc("dw", L, State) ->
	{ok, N, L2} = tok_num(L),
	State2 = mem_write(State, [
		{calc, {unop, o_low, N}},
		{calc, {unop, o_high, N}}]),
	L3 = skip_ws(L2),
	case L3 of
		[$,|Lx2] ->
			parse_direc("dw", skip_ws(Lx2), State2);
		_ ->
			parse_end(L3),
			State2
	end;

parse_direc("db", L, State) ->
	{WList, L2} = case L of
		[$" | _] ->
			{ok, S, Lx1} = tok_str(L),
			{S, Lx1};
		_ ->
			{ok, N, Lx1} = tok_num(L),
			{[{calc, {unop, o_low, N}}], Lx1}
	end,
	State2 = mem_write(State, WList),
	L3 = skip_ws(L2),
	case L3 of
		[$,|Lx2] ->
			parse_direc("db", skip_ws(Lx2), State2);
		_ ->
			parse_end(L3),
			State2
	end;

parse_direc("segment", L, State) ->
	{ok, Idx, L2} = tok_num(L),
	{ok, Val, L3} = tok_num(skip_ws(L2)),
	parse_end(skip_ws(L3)),

	io:format("segment ~p ~p~n", [Idx, Val]),
	
	% TODO!

	State;

parse_direc("org", L, State) ->
	{ok, P, L2} = tok_num(L),
	parse_end(skip_ws(L2)),
	State#asm_state{
		pos = P,
		org = case State#asm_state.org of
			nil -> P;
			O -> case P < O of
				true -> P;
				false -> O
			end
		end
	};

parse_direc("align", L, State) ->
	{ok, Al, L2} = tok_num(L),
	parse_end(skip_ws(L2)),
	ensure_pow2(Al),
	P = (State#asm_state.pos + Al - 1) band (16#FFFFFFFF bxor (Al - 1)),
	State#asm_state{
		pos = P,
		org = case State#asm_state.org of
			nil -> P;
			O -> case P < O of
				true -> P;
				false -> O
			end
		end
	}.

%
% Op parsing functions
%

parse_op0(Code, L, State) ->
	% OP0
	State2 = mem_write(State, [
		2#00000000 + Code]),
	L1 = skip_ws(L),
	parse_end(L1),
	State2.

parse_op1(Code, [$., S|L1], State) when ?IS_W_B(S) ->
	% OP1.l @x, @y
	% OP1.l @x, @y, #i
	% OP1.l @x, #i <-- implicit y is set to x

	% .l
	Size = opsize(S),

	% @x
	{ok, RegN1, L2} = tok_reg(skip_ws(L1)),
	% TODO: cleaner seg get/set
	% true = (RegN1 /= 0 orelse Code /= op1("move")),

	% ,
	[$, | L3] = skip_ws(L2),

	% @y or #i
	{L4, State2} = case skip_ws(L3) of
		Lx1 = [$@ | _] ->
			% @y
			{ok, RegN2, Lx2} = tok_reg(Lx1),

			% check for , #i
			case skip_ws(Lx2) of
				[$#, Ly1] ->
					% #i
					{ok, Imm, Ly2} = tok_num(skip_ws(Ly1)),
					Ly3 = skip_ws(Ly2),
					Sy1 = mem_write(State, [
						2#01100000 + 16*Size + Code,
						RegN1*16 + RegN2]),
					Sy2 = mem_write_imm_size(Sy1, Size+1, Imm),
					{Ly3, Sy2};
				Ly1 ->
					% end of op
					Sy1 = mem_write(State, [
						2#01000000 + 16*Size + Code,
						RegN1*16 + RegN2]),
					{Ly1, Sy1}
			end;
		[$# | Lx1] ->
			% #i
			{ok, Imm, Lx2} = tok_num(Lx1),
			Sx1 = mem_write(State, [
				2#01100000 + 16*Size + Code,
				RegN1*16 + RegN1]),
			Sx2 = mem_write_imm_size(Sx1, Size+1, Imm),
			{Lx2, Sx2}
	end,
	L5 = skip_ws(L4),
	parse_end(L5),
	State2.

parse_op2(Code, [$., VS, $:, VSeg|L1], State) when ?IS_W_B(VS) ->
	% LD.l:s @x, @y
	% LD.l:=[^] @x, $baaaa
	% LD.l:s[<] @x, $aa[, @y]
	% LD.l:s[>] @x, $aaaa[, @y]
	% LD.l:s[^] @x, $baaaa
	% ST.l:s @y, @x
	% ST.l:=[^] $baaaa, @x
	% ST.l:s[<] $aa[, @y], @x
	% ST.l:s[>] $aaaa[, @y], @x
	% ST.l:s[^] $baaaa, @x

	% .l
	Size = opsize(VS),

	% :s/:=
	{Seg, AbsState} = case VSeg of
		$= -> {3, true};
		$3 -> {3, false};
		C when C >= $0, C =< $3 -> {C-$0, nil}
	end,

	{Hint, L1a} = case L1 of
		[$^ | L1ax] -> {far, L1ax};
		[$> | L1ax] -> {near, L1ax};
		[$< | L1ax] -> {short, L1ax};
		_ -> {nil, L1}
	end,

	% ASSERTIONS.
	true = (Hint == nil orelse Hint /= far orelse AbsState /= false),
	true = (Hint == nil orelse Hint == far orelse AbsState /= true),

	{L2, RegN1, RegN2, ImmV} = case Code of
		0 ->
			% LD
			% @x
			{ok, LRegN1, Lx1} = tok_reg(skip_ws(L1a)),
			[$, | Lx2] = skip_ws(Lx1),
			
			case skip_ws(Lx2) of
				Ly1 = [$@ | _] ->
					% @y
					{ok, LRegN2, Ly2} = tok_reg(skip_ws(Ly1)),
					case skip_ws(Ly2) of
						[$, | Lz1] ->
							Lz2 = skip_ws(Lz1),
							{ok, LImm, Lz3} = tok_num(Lz2),
							{skip_ws(Lz3), LRegN1, LRegN2, LImm};
						Lz1 -> {Lz1, LRegN1, LRegN2, 0}
					end;
				Ly1 ->
					% immediate
					{ok, LImm, Ly2} = tok_num(skip_ws(Ly1)),
					case skip_ws(Ly2) of
						[$, | Lz1] ->
							% @y also
							{ok, LRegN2, Lz2} = tok_reg(skip_ws(Lz1)),
							{skip_ws(Lz2), LRegN1, LRegN2, LImm};
						Lz1 ->
							% end
							{Lz1, LRegN1, 0, LImm}
					end
			end;
		1 ->
			% ST
			% check which we have
			case skip_ws(L1a) of
				Lx1 = [$@ | _] ->
					% @y, [imm, ]@x
					{ok, LRegN2, Lx2} = tok_reg(skip_ws(Lx1)),
					[$, | Lx3] = skip_ws(Lx2),

					% next will either be a reg or an immediate followed by a reg
					case skip_ws(Lx3) of
						Ly1 = [$@ | _] ->
							% @x
							{ok, LRegN1, Ly2} = tok_reg(Ly1),
							{skip_ws(Ly2), LRegN1, LRegN2, 0};
						Ly1 ->
							% imm, @x
							{ok, LImm, Ly2} = tok_num(Ly1),
							[$, | Ly3] = skip_ws(Ly2),
							{ok, LRegN1, Ly4} = tok_reg(skip_ws(Ly3)),
							{skip_ws(Ly4), LRegN1, LRegN2, LImm}
					end;
				Lx1 ->
					% immediate
					{ok, LImm, Lx2} = tok_num(skip_ws(Lx1)),
					[$, | Lx3] = skip_ws(Lx2),

					% next will be either @y,@x or just @x
					{ok, LRegFirst, Lx4} = tok_reg(skip_ws(Lx3)),

					case skip_ws(Lx4) of
						[$, | Ly1] ->
							% @y, @x
							{ok, LRegSecond, Ly2} = tok_reg(skip_ws(Ly1)),
							{skip_ws(Ly2), LRegSecond, LRegFirst, LImm};
						Ly1 ->
							% @x
							{skip_ws(Ly1), LRegFirst, 0, LImm}
					end
			end
	end,

	Imm = calc(State, ImmV, false),

	io:format("op2 ~p ~p ~p ~p ~p ~p~n", [RegN1, RegN2, Imm, Seg, AbsState, Hint]),

	Mode = case {RegN1, RegN2, Imm, Seg, AbsState, Hint} of
		{_, _, 0, _, X, nil} when X /= true -> reg;
		{_, 0, _, _, X, far} when X /= false, is_integer(Imm) -> far;
		{_, _, _, _, _, short} when is_integer(Imm), ((Imm band 16#FFFF) >= 16#FF80 orelse (Imm band 16#FFFF) < 16#80) -> short;
		{_, _, _, _, _, near} when is_integer(Imm) -> near;
		{_, 0, _, _, X, nil} when X /= false, is_integer(Imm) -> far;
		{_, _, _, _, X, nil} when X /= true, is_integer(Imm), ((Imm band 16#FFFF) >= 16#FF80 orelse (Imm band 16#FFFF) < 16#80) -> short;
		{_, _, _, _, X, nil} when X /= true -> near
	end,

	parse_end(skip_ws(L2)),

	case Mode of
		reg ->
			mem_write(State, [
				2#10000000 + Code*8 + Size*4 + Seg,
				RegN1*16 + RegN2]);
		short ->
			mem_write(State, [
				2#10100000 + Code*8 + Size*4 + Seg,
				RegN1*16 + RegN2,
				{calc, {unop, o_low, Imm}}]);
		near ->
			mem_write(State, [
				2#10110000 + Code*8 + Size*4 + Seg,
				RegN1*16 + RegN2,
				{calc, {unop, o_low, Imm}},
				{calc, {unop, o_high, Imm}}]);
		far ->
			mem_write(State, [
				2#10010000 + Code*8 + Size*4 + Seg,
				{calc, {binop, o_add, RegN1*16, {unop, o_top4, Imm}}},
				{calc, {unop, o_low, Imm}},
				{calc, {unop, o_high, Imm}}])
	end.

parse_op3(Code, L1, State) ->
	% OP3= $baaaa[, @x]
	% OP3: $aaaa[, @x]
	% OP3 $aa[aa]

	{Hint, L1a} = case L1 of
		[$= | L1ax] -> {abs, L1ax};
		[$* | L1ax] -> {abs_seg, L1ax};
		[$< | L1ax] -> {short, L1ax};
		[$> | L1ax] -> {near, L1ax};
		_ -> {nil, L1}
	end,

	% read address
	{ok, ImmV, L2} = tok_num(skip_ws(L1a)),
	Imm = calc(State, ImmV, false),

	% check if we have a register following
	{L3, RegN1} = case skip_ws(L2) of
		[$, | Lx1] ->
			% @x
			{ok, LRegN1, Lx2} = tok_reg(skip_ws(Lx1)),
			{skip_ws(Lx2), LRegN1};
		Lx1 -> {Lx1, 0}
	end,

	io:format("op3 ~p ~p ~p ~p ~p~n", [Code, Imm, RegN1, Hint, L3]),

	RelImmTemp = State#asm_state.pos + 2,
	Mode = case {Imm, RegN1, Hint} of
		{_, _, abs} -> abs;
		{_, 0, abs_seg} -> abs_seg;
		{_, 0, short} -> short;
		{_, 0, near} -> near;
		{_, 0, nil} -> near % TODO: optimise properly, determine whether short or near
	end,

	case Mode of
		abs ->
			mem_write(State, [
				2#11000000 + Code,
				{calc, {binop, o_add, RegN1*16, {unop, o_top4, Imm}}},
				{calc, {unop, o_low, Imm}},
				{calc, {unop, o_high, Imm}}]);
		abs_seg ->
			mem_write(State, [
				2#11010000 + Code,
				{calc, {unop, o_low, Imm}},
				{calc, {unop, o_high, Imm}}]);
		short ->
			RelImm = State#asm_state.pos + 2,
			mem_write(State, [
				2#11110000 + Code,
				{calc, {unop, o_low, {binop, o_sub, Imm, RelImm}}}]);
		near ->
			RelImm = State#asm_state.pos + 3,
			mem_write(State, [
				2#11110000 + Code,
				{calc, {unop, o_low, {binop, o_sub, Imm, RelImm}}},
				{calc, {unop, o_high, {binop, o_sub, Imm, RelImm}}}])
	end.

%
% Line parsing
%
parse_asm_line([], State) -> State;
parse_asm_line([$; | _], State) -> State;
parse_asm_line([C | T], State) when ?IS_WS(C) -> parse_asm_line(T, State);
parse_asm_line(L1 = [C | _], State) when ?IS_LETTER(C) orelse C == $_ ->
	{ok, Name, L2} = tok_name(L1),
	%io:format("name token ~p, trailing ~p~n", [Name, L2]),
	%io:format("state ~p~n", [State]),
	case L2 of
		[$: | T2] ->
			State2 = label_add(State, Name),
			parse_asm_line(skip_ws(T2), State2);
		_ -> case optype(string:to_lower(Name)) of
			{op0, X} -> parse_op0(X, L2, State);
			{op1, X} -> parse_op1(X, L2, State);
			{op2, X} -> parse_op2(X, L2, State);
			{op3, X} -> parse_op3(X, L2, State);
			error -> error(lists:flatten(io_lib:format("invalid opcode ~p", [Name])))
		end
	end;
parse_asm_line([$. | L1 = [C | _]], State) when ?IS_LETTER(C) ->
	{ok, Name, L2} = tok_name(L1),
	L3 = skip_ws(L2),
	
	%io:format("directive ~p trailing ~p~n", [Name, L3]),
	parse_direc(Name, L3, State);
parse_asm_line(L, State) ->
	io:format("todo line ~p ~p~n", [L, State]),
	State. % TODO

%
% Loading the files and the main parsing loop and all that crap.
%

open_asm(FName) ->
	io:format("[asm] Reading ~p~n", [FName]),
	{ok, L} = file:read_file(FName),
	binary_to_list(L).

get_asm_line([], Acc) -> {lists:reverse(Acc), []};
get_asm_line([C | L], Acc) when C == $\n orelse C == $\r -> {lists:reverse(Acc), L};
get_asm_line([H | T], Acc) -> get_asm_line(T, [H | Acc]).

get_asm_line(L) -> get_asm_line(L, []).

parse_asm([], State) -> State;
parse_asm(L, State) ->
	{LParse, L2} = get_asm_line(L),
	S2 = parse_asm_line(LParse, State),
	parse_asm(L2, S2).

parse_asm_string(Str) ->
	parse_asm(Str, #asm_state{labels=dict:new()}).

parse_asm_file(FName) ->
	L = open_asm(FName),
	parse_asm_string(L).

main([Type, FName, OutFName]) when Type == "rom" ->
	State = parse_asm_file(FName),
	io:format("Assembly parsed~n"),
	{Org, Data} = case Type of
		"rom" -> mem_dump(State)
	end,
	io:format("Memory collected at location ~p, ~p bytes~n", [Org, length(Data)]),
	file:write_file(OutFName, list_to_binary(Data)).

main() -> main([]).

