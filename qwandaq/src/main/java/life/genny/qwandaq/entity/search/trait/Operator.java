package life.genny.qwandaq.entity.search.trait;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public enum Operator {
	IN,
	NOT_IN,
	LIKE,
	NOT_LIKE,
	EQUALS,
	NOT_EQUALS,
	GREATER_THAN,
	GREATER_THAN_OR_EQUAL,
	LESS_THAN_OR_EQUAL,
	LESS_THAN,
	CONTAINS,
	NOT_CONTAINS,
	STARTS_WITH,
	NOT_STARTS_WITH
}