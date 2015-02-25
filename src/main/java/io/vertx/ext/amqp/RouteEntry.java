package io.vertx.ext.amqp;

import java.util.List;
import java.util.regex.Pattern;

public interface RouteEntry
{
    public void add(String addr);

    public void remove(String addr);

    public Pattern getPattern();

    public List<String> getAddressList();
}