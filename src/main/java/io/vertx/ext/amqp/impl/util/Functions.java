package io.vertx.ext.amqp.impl.util;

public class Functions
{
    public static String format(String format, Object... args)
    {
        return String.format(format, args);
    }

    public static void print(String format, Object... args)
    {
        System.out.println(format(format, args));
    }
}