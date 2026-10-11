package io.github.eggy03.yabb.bot;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public final class Start {
    static void main(String... args) {
        Quarkus.run(args);
    }
}
