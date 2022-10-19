package me.vjgallego.pdfgenerator.model;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class Metadata {
    private List<String> variables;

    public List<String> getVariables() {
        return variables;
    }

    public Metadata setVariables(List<String> variables) {
        this.variables = variables;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Metadata metadata = (Metadata) o;
        return Objects.equals(variables, metadata.variables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variables);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Metadata.class.getSimpleName() + "[", "]")
                .add("variables=" + variables)
                .toString();
    }
}
