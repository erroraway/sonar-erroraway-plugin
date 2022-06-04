package com.bug;

public class VarArgsArray {
    public void test() {
        a(12, 34D, 56L);
    }
    
    public <N extends Number> int a(N ... values) {
        return values.length;
    }
}