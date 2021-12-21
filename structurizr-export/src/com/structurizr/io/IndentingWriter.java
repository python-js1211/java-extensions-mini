package com.structurizr.io;

public final class IndentingWriter {

    private int indent = 0;
    private IndentType indentType = IndentType.Spaces;
    private int indentQuantity = 2;

    private StringBuilder buf = new StringBuilder();

    public IndentingWriter() {
    }

    public void setIndentType(IndentType indentType) {
        this.indentType = indentType;
    }

    public void setIndentQuantity(int indentQuantity) {
        this.indentQuantity = indentQuantity;
    }

    public void indent() {
        indent++;
    }

    public void outdent() {
        indent--;
    }

    private String padding() {
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < indent * indentQuantity; i++) {
            if (indentType == IndentType.Spaces) {
                buf.append(" ");
            } else {
                buf.append("\t");
            }
        }

        return buf.toString();
    }

    public void writeLine() {
        buf.append("\n");
    }

    public void writeLine(String content) {
        buf.append(String.format("%s%s\n", padding(), content.replace("\n", "\\n")));
    }

    @Override
    public String toString() {
        return buf.toString();
    }

}