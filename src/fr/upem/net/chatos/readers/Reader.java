package fr.upem.net.chatos.readers;

public interface Reader<T> {

    public static enum ProcessStatus {DONE,REFILL,ERROR};

    public ProcessStatus process();

    public T get();

    public void reset();

}
