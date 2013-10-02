% Compiler for Areia-1
% by Ben "GreaseMonkey" Russell, 2013
% Licence: CC0: http://creativecommons.org/publicdomain/zero/1.0/

-module(cmp).
-export([main/0, main/1]).
-record(state, {}).

open_asm(FName) ->
	io:format("[cmp] Reading ~p~n", [FName]),
	{ok, L} = file:read_file(FName),
	binary_to_list(L).

parse_cmp_file(FName) ->
	error(todo).

main([FName, OutFName]) ->
	Str = parse_cmp_file(FName),
	io:format("Code compiled~n"),
	StateAsm = asm:parse_asm_string(Str),
	io:format("Assembly parsed~n"),
	{Org, Data} = mem_dump(State),
	io:format("Memory collected at location ~p, ~p bytes~n", [Org, length(Data)]),
	file:write_file(OutFName, list_to_binary(Data)).

main() -> main([]).

