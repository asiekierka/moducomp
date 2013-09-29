% Areia-1 CPU assembler
% by Ben "GreaseMonkey" Russell, 2013
% Licence: CC0: http://creativecommons.org/publicdomain/zero/1.0/
%
% NOTES:
% - We don't support expressions yet.
% - Most of the time, if you get a syntax error, you'll just get an erlang badmatch.
-module(asm).
-export([main/0, main/1]).
-record(state, {org=nil, eof=nil, pos=16#00000, labels=nil, mem=nil}).

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

op1("move") -> 0;
op1("cmp")  -> 1;
op1("add")  -> 2;
op1("sub")  -> 3;
op1("xor")  -> 4;
op1("or")   -> 5;
op1("and")  -> 6;
op1(_) -> nil.

op2("asl") -> 0; op2("lsl") -> 0;
op2("asr") -> 1;
op2("lsr") -> 2;
op2("rol") -> 3;
op2("ror") -> 4;
op2("rcl") -> 5;
op2("rcr") -> 6;
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

op4("ld") -> 0;
op4("st") -> 1;
op4(_) -> nil.

ops("nop")   -> 0;
ops("ret")   -> 2;
ops("popf")  -> 4;
ops("pushf") -> 6;
ops("cli")   -> 8;
ops("sei")   -> 10;
ops("hlt")   -> 12;
ops(_) -> nil.

optype(X, nil, nil, nil, nil) when X /= nil -> {op1, X};
optype(nil, X, nil, nil, nil) when X /= nil -> {op2, X};
optype(nil, nil, X, nil, nil) when X /= nil -> {op3, X};
optype(nil, nil, nil, X, nil) when X /= nil -> {op4, X};
optype(nil, nil, nil, nil, X) when X /= nil -> {ops, X};
optype(_, _, _, _, _) -> error.

optype(X) -> optype(op1(X), op2(X), op3(X), op4(X), ops(X)).

%
% Calculation tree
%

label_add(State, Name) ->
	false = dict:is_key(Name, State#state.labels),

	State#state {
		labels = dict:store(Name, State#state.pos, State#state.labels)
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
	case dict:is_key(Name, State#state.labels) of
		true -> dict:fetch(Name, State#state.labels);
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
calc(State, {calc, C}, AtEnd) ->
	calc(State, C, AtEnd).

%
% Memory access
%

mem_dump(State) ->
	Org = State#state.org,
	Eof = State#state.eof,
	Mem = State#state.mem,

	Data = lists:reverse(lists:foldl(fun (I, Acc) ->
		[calc(State, array:get(I, Mem), true)|Acc]
	end, [], lists:seq(Org, Eof-1))),
	{Org, Data}.

% mem_seek(State, Addr) ->
% 	State#state{pos = Addr}.

mem_write(State, []) -> State;
mem_write(State, [H|T]) ->
	M1 = case State#state.mem of
		nil -> array:new([{default, 16#FF}]);
		Mx -> Mx
	end,

	P1 = State#state.pos,
	%io:format("write ~p ~p~n", [P1, H]),
	M2 = array:set(P1, calc(State, H, false), M1),
	P2 = (P1 + 1),

	O1 = case State#state.org of
		nil -> P1;
		_ -> State#state.org
	end,
	E1 = case State#state.eof of
		nil -> P2;
		_ -> case (P2 > State#state.eof) of
			true -> P2;
			false -> State#state.eof
		end
	end,

	State2 = State#state{
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
tok_str([$\\, $n | T], Acc) -> tok_str(T, [$n | Acc]);
tok_str([$\\, $r | T], Acc) -> tok_str(T, [$r | Acc]);
tok_str([$\\, $t | T], Acc) -> tok_str(T, [$t | Acc]);
tok_str([$\\, $b | T], Acc) -> tok_str(T, [$b | Acc]);
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
tok_int(L = [C|_]) when ?IS_DIGIT(C) -> tok_int_dec(L, 0);
tok_int(_) -> error.

tok_name([C|T], Acc) when ?IS_LETTER(C) orelse C == $_ orelse ?IS_DIGIT(C) -> tok_name(T, [C|Acc]);
tok_name(L, Acc) -> {ok, lists:reverse(Acc), L}.

tok_name(L = [C|_]) when ?IS_LETTER(C) orelse C == $_ -> tok_name(L, []);
tok_name(_) -> error.

tok_num_lit(L = [C|_]) when ?IS_LETTER(C) orelse C == $_ ->
	{ok, Name, L1} = tok_name(L),
	{ok, {calc, {label, Name}}, L1};
tok_num_lit(L) -> tok_int(L).

% TODO: expressions
tok_num(L) -> tok_num_lit(L).

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

parse_direc("org", L, State) ->
	{ok, P, L2} = tok_num(L),
	parse_end(skip_ws(L2)),
	State#state{
		pos = P,
		org = case State#state.org of
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
	P = (State#state.pos + Al - 1) band (16#FFFFFFFF bxor (Al - 1)),
	State#state{
		pos = P,
		org = case State#state.org of
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

parse_op1(Code, [$., S|L1], State) when ?IS_W_B(S) ->
	% @x, @y
	% @x, #i
	Size = opsize(S),
	{ok, RegN1, L2} = tok_reg(skip_ws(L1)),
	true = (RegN1 /= 0),
	[$, | L3] = skip_ws(L2),
	{L4, State2} = case skip_ws(L3) of
		Lx1 = [$@ | _] ->
			{ok, RegN2, Lx2} = tok_reg(Lx1),
			Sx1 = mem_write(State, [
				2#11100000 + 16*Size + Code,
				RegN1 + RegN2*16]),
			{Lx2, Sx1};
		[$# | Lx1] ->
			{ok, Imm, Lx2} = tok_num(Lx1),
			Sx1 = mem_write(State, [
				16*Size + 32*Code + RegN1]),
			Sx2 = mem_write_imm_size(Sx1, Size+1, Imm),
			{Lx2, Sx2}
	end,
	L5 = skip_ws(L4),
	parse_end(L5),
	State2.

parse_op2(Code, [$., S|L1], State) when S == $w orelse S == $W ->
	% @x, @y
	% @x, #i
	{ok, RegN1, L2} = tok_reg(skip_ws(L1)),
	true = (RegN1 /= 0),
	[$, | L3] = skip_ws(L2),
	{L4, State2} = case skip_ws(L3) of
		Lx1 = [$@ | _] ->
			{ok, RegN2, Lx2} = tok_reg(Lx1),
			Sx1 = mem_write(State, [
				2#11101000 + Code,
				RegN1 + RegN2*16]),
			{Lx2, Sx1};
		[$# | Lx1] ->
			{ok, Imm, Lx2} = tok_num(Lx1),
			true = (Imm >= 0 andalso Imm =< 15),
			Sx1 = mem_write(State, [
				2#11111000 + Code,
				RegN1 + Imm*16]),
			{Lx2, Sx1}
	end,
	L5 = skip_ws(L4),
	parse_end(L5),
	State2.

parse_op3(Code, L, State) ->
	{ok, Imm, L1} = tok_num(skip_ws(L)),
	L2 = skip_ws(L1),
	{State2, L3} = case L2 of
		[$, | Lx1] ->
			Lx2 = skip_ws(Lx1),
			{ok, RegN, Lx3} = tok_reg(Lx2),
			0 = (Imm band 16#FFF),
			Sx1 = mem_write(State, [
				2#11100111,
				Code*16 + RegN,
				{calc, {unop, o_top8, Imm}}]),
			{Sx1, Lx3};
		_ ->
			Sx1 = case Imm of
				_ when is_integer(Imm) andalso (Imm band 16#FFF) == 0 -> mem_write(State, [
					2#11100111,
					Code*16 + 0,
					{calc, {unop, o_top8, Imm}}]);
				_ -> mem_write(State, [
					2#11110111,
					{calc, {binop, o_add, Code*16, {unop, o_top4, Imm}}},
					{calc, {unop, o_low, Imm}},
					{calc, {unop, o_high, Imm}}])
			end,
			{Sx1, L2}
	end,
	parse_end(skip_ws(L3)),
	State2.

parse_op4_pair(L, _State, RegFollow) ->
	{ok, Imm, L1} = tok_num(L),
	L2 = skip_ws(L1),
	case L2 of
		[$, | Lx1] -> 
			Lx2 = skip_ws(Lx1),
			{ok, RegN2, Lx3} = tok_reg(Lx2),
			Lx4 = skip_ws(Lx3),
			case {RegFollow, Lx4} of
				{false, _} -> {Imm, RegN2, Lx4};
				{true, [$, | Ly1]} -> {Imm, RegN2, skip_ws(Ly1)};
				_ -> {Imm, 0, Lx2}
			end;
		[$; | _] -> {Imm, 0, L2};
		[] -> {Imm, 0, L2}
	end.

parse_op4_spec(0, L1, State) ->
	{ok, RegN1, L2} = tok_reg(skip_ws(L1)),
	[$, | L3] = skip_ws(L2),
	L4 = skip_ws(L3),
	{Imm, RegN2, L5} = parse_op4_pair(L4, State, false),
	{RegN1, Imm, RegN2, L5};
parse_op4_spec(1, L1, State) ->
	{Imm, RegN2, L2} = parse_op4_pair(skip_ws(L1), State, true),
	{ok, RegN1, L3} = tok_reg(skip_ws(L2)),
	{RegN1, Imm, RegN2, L3}.

parse_op4(Code, [$., S|L1], State) when ?IS_W_B(S) ->
	% LD.* @x, $Faaaa
	% LD.* @x, $baa00, @y
	% LD.* @x, $baaaa, @y
	% ST.* $Faaaa, @x
	% ST.* $baa00, @y, @x
	% ST.* $baaaa, @y, @x

	Size = opsize(S),
	{RegN1, ImmV, RegN2, L2} = parse_op4_spec(Code, L1, State),
	Imm = ImmV band 16#FFFFF,

	S2 = mem_write(State, [
		2#11101111 + Size*16]),
	
	S3 = case {Imm, RegN2} of
		{_, 0} when Imm >= 16#F0000 andalso Imm =< 16#FFFFF ->
			mem_write(S2, [
				2#00100000 + Code*16 + RegN1,
				Imm band 16#FF,
				(Imm bsr 8) band 16#FF]);
		{_, _} when (Imm band 16#000FF) == 0 ->
			mem_write(S2, [
				2#01000000 + Code*16 + RegN1,
				((Imm bsr 16) band 16#F) + RegN2*16,
				(Imm bsr 8) band 16#FF]);
		_ ->
			mem_write(S2, [
				2#01100000 + Code*16 + RegN1,
				((Imm bsr 16) band 16#F) + RegN2*16,
				Imm band 16#FF,
				(Imm bsr 8) band 16#FF])
	end,
	L3 = skip_ws(L2),
	parse_end(L3),
	S3.

parse_ops(Code, L1, State) ->
	State2 = mem_write(State, [
		2#00000000 + Code*16]),
	L2 = skip_ws(L1),
	parse_end(L2),
	State2.

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
			{op1, X} -> parse_op1(X, L2, State);
			{op2, X} -> parse_op2(X, L2, State);
			{op3, X} -> parse_op3(X, L2, State);
			{op4, X} -> parse_op4(X, L2, State);
			{ops, X} -> parse_ops(X, L2, State);
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
	io:format("Reading ~p~n", [FName]),
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

parse_asm(FName) ->
	L = open_asm(FName),
	parse_asm(L, #state{labels=dict:new()}).

main([FName, OutFName]) ->
	State = parse_asm(FName),
	io:format("Assembly parsed~n"),
	{Org, Data} = mem_dump(State),
	io:format("Memory collected at location ~p, ~p bytes~n", [Org, length(Data)]),
	file:write_file(OutFName, list_to_binary(Data)).

main() -> main([]).

