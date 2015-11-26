package me.bazhenov.groovysh


class Greeter {

    private final String name

    public Greeter(String name) {
        this.name = name
    }

    public String hello() {
        return String.format("Greetings %s!!!", name)
    }

}
