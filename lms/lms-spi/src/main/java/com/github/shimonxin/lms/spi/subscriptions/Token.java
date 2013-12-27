package com.github.shimonxin.lms.spi.subscriptions;

public class Token {

	public static final Token EMPTY = new Token("");
	public static final Token MULTI = new Token("#");
	public static final Token SINGLE = new Token("+");
    String name;

    public Token(String s) {
        name = s;
    }

    public String name() {
        return name;
    }

    public boolean match(Token t) {
        if (t == MULTI || t == SINGLE) {
            return false;
        }

        if (this == MULTI || this == SINGLE) {
            return true;
        }

        return equals(t);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Token other = (Token) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return name;
    }
}