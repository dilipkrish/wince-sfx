package com.github.dilipkrish.zip;

public class Application {

    public static void main(String[] args) throws Exception {
        new CreateSelfExtractingZipCommand().run(args);
    }

}
