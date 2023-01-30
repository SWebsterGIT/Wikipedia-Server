package cpen221.mp3.testing;

import cpen221.mp3.fsftbuffer.Bufferable;

import java.util.Objects;

public class TestT implements Bufferable {

    private final String value;

    public TestT(String input){
        this.value = input;
    }

    public String getValue(){
        return this.value;
    }

    @Override
    public String id() {
        return this.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestT testT = (TestT) o;
        return Objects.equals(value, testT.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
