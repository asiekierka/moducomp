% Compiler for Areia-1
% by Ben "GreaseMonkey" Russell, 2013
% Licence: CC0: http://creativecommons.org/publicdomain/zero/1.0/

-module(cmp).
-export([main/0, main/1]).
-record(state, {}).
-define(IS_NAME_START(C), ((C >= $a andalso C =< $z) orelse (C >= $A andalso C =< $Z) orelse C == $_)).
-define(IS_NAME_CHAR(C), ((C >= $0 andalso C =< $9) orelse ?IS_NAME_START(C))).
-define(IS_NUMBER_START(C), (C >= $0 andalso C =< $9)).
-define(IS_DIGIT(C), (C >= $0 andalso C =< $9)).
-define(IS_UNOP1(C), (C == $- orelse C == $~ orelse C == $+ orelse C == $* orelse C == $&)).

oprec(".") -> 1;
oprec("->") -> 1;

oprec("*") -> 3;
oprec("/") -> 3;
oprec("%") -> 3;

oprec("+") -> 4;
oprec("-") -> 4;

oprec("<<") -> 5;
oprec(">>") -> 5;

oprec("<")  -> 6;
oprec("<=") -> 6;
oprec(">")  -> 6;
oprec(">=") -> 6;

oprec("==") -> 7;
oprec("!=") -> 7;

oprec("&")  -> 8;
oprec("^")  -> 9;
oprec("|")  -> 10;
oprec("&&") -> 11;
oprec("||") -> 12;

oprec("=")   -> 13;
oprec("+=")  -> 13;
oprec("-=")  -> 13;
oprec("*=")  -> 13;
oprec("/=")  -> 13;
oprec("%=")  -> 13;
oprec("&=")  -> 13;
oprec("|=")  -> 13;
oprec("^=")  -> 13;
oprec("<<=") -> 13;
oprec(">>=") -> 13;
oprec("&&=") -> 13;
oprec("||=") -> 13;

oprec(",") -> 14;

oprec(_) -> nil.

oprec_force_value(V) when is_number(V) -> V.
oprec_force(Op) -> oprec_force_value(oprec(Op)).

skip_ws([C|T]) when C == 32; C == $\n; C == $\r; C == $\t -> skip_ws(T);
skip_ws(L) -> L.

tok_name([C|T], Acc) when ?IS_NAME_CHAR(C) -> tok_name(T, [C|Acc]);
tok_name(L, Acc) -> {L, lists:reverse(Acc)}.

tok_name(L = [C|_]) when ?IS_NAME_START(C) ->
	tok_name(L, []).

tok_num_encaps_dec([C|T], N) when ?IS_DIGIT(C) -> tok_num_encaps_dec(T, N*10+(C-$0));
tok_num_encaps_dec(L, N) -> {L, {int, N}}.

% TODO: hex, oct
tok_num_encaps(L = [C|_]) when ?IS_DIGIT(C) -> tok_num_encaps_dec(L, 0).

open_cmp(FName) ->
	io:format("[cmp] Reading ~p~n", [FName]),
	{ok, L} = file:read_file(FName),
	binary_to_list(L).

% shunt(State, Toks, Ops, TokStack, OpStack)
% shunt(State, Toks, Ops)
shunt(State, [Tok], []) -> {Tok, State};
shunt(State, [T1,T2], [Op]) -> {{binop, Op, T1, T2}, State};
shunt(State, [T1,T2,T3 | TT], [O1,O2 | OT]) ->
	P1 = oprec_force(O1),
	P2 = oprec_force(O2),

	%
	case P1 =< P2 of
		true ->
			% Easy case: Bind immediately.
			shunt(State, [{binop, O1, T1, T2}, T3 | TT], [O2 | OT]);
		false ->
			% Hard case: Push it onto the stack.
			error(todo)
	end.

parse_exp(L = [C|_], State) when ?IS_NUMBER_START(C) ->
	{L2, RTree} = tok_num_encaps(L),
	{skip_ws(L2), RTree, State};
parse_exp(L = [C|_], State) when ?IS_NAME_START(C) ->
	% Check if it's a special keyword first.
	{L2, Name} = tok_name(L),
	case Name of
		_ ->
			{skip_ws(L2), {ref, {name, Name}}, State}
	end.

parse_exp_chain(L, State, Toks, Ops) ->
	{L2, RTree, State2} = parse_exp(L, State),
	case L2 of
		[$;|Lx1] ->
			{RTree2, State3} = shunt(State, Toks ++ [RTree], Ops),
			{skip_ws(Lx1), RTree2, State3}
	end.

%parse_stat(L = [${|_], State) ->
%	{RTree, State2} = parse_block(L, State),
%	{{block, RTree}, State2};
parse_stat(L = [C|_], State) when ?IS_NUMBER_START(C) orelse ?IS_UNOP1(C) ->
	parse_exp_chain(L, State, [], []);
parse_stat(L = [C|_], State) when ?IS_NAME_START(C) ->
	% Check if it's a special keyword first.
	{L2, Name} = tok_name(L),
	{L3, RTree, State2} = case Name of
		_ ->
			{Lx1, Rx1, Sx1} = parse_exp_chain(L, State, [], []),
			{skip_ws(Lx1), Rx1, Sx1}
	end.

% WARNING: O(n^2)!
parse_stat_list([], State) -> {{term}, State};
parse_stat_list(L, State) ->
	{L2, RTree, State2} = parse_stat(L, State),
	{T3, State3} = parse_stat_list(skip_ws(L2), State2),
	{{stat_pair, RTree, T3}, State3}.

parse_cmp_file(FName) ->
	R = parse_stat_list(skip_ws(open_cmp(FName)), #state{}),
	io:format("~p~n", [R]),
	R.

main([FName, OutFName]) ->
	Str = parse_cmp_file(FName),
	io:format("Code compiled~n"),
	StateAsm = asm:parse_asm_string(Str),
	io:format("Assembly parsed~n"),
	{Org, Data} = asm:mem_dump(StateAsm),
	io:format("Memory collected at location ~p, ~p bytes~n", [Org, length(Data)]),
	file:write_file(OutFName, list_to_binary(Data)).

main() -> main([]).

