package io.spacedog.client.elastic;

public class LuceneSimpleQueryParserConstants {

	/** Enables {@code AND} operator (+) */
	public static final int AND_OPERATOR = 1 << 0;
	/** Enables {@code NOT} operator (-) */
	public static final int NOT_OPERATOR = 1 << 1;
	/** Enables {@code OR} operator (|) */
	public static final int OR_OPERATOR = 1 << 2;
	/** Enables {@code PREFIX} operator (*) */
	public static final int PREFIX_OPERATOR = 1 << 3;
	/** Enables {@code PHRASE} operator (") */
	public static final int PHRASE_OPERATOR = 1 << 4;
	/** Enables {@code PRECEDENCE} operators: {@code (} and {@code )} */
	public static final int PRECEDENCE_OPERATORS = 1 << 5;
	/** Enables {@code ESCAPE} operator (\) */
	public static final int ESCAPE_OPERATOR = 1 << 6;
	/** Enables {@code WHITESPACE} operators: ' ' '\n' '\r' '\t' */
	public static final int WHITESPACE_OPERATOR = 1 << 7;
	/** Enables {@code FUZZY} operators: (~) on single terms */
	public static final int FUZZY_OPERATOR = 1 << 8;
	/** Enables {@code NEAR} operators: (~) on phrases */
	public static final int NEAR_OPERATOR = 1 << 9;
}
