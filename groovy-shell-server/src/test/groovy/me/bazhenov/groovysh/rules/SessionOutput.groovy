package me.bazhenov.groovysh.rules


class SessionOutput extends ByteArrayOutputStream {

    public boolean contains(String data) {
        return toString().contains(data);
    }

    @Override
    public String toString() {
        return normalized(super.toString("UTF-8"))
    }

    private static String normalized(String data) {
        return data.replaceAll("\\e\\[[\\d;]*[^\\d;]","")
    }

}
