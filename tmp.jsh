import java.net.*;
var uri = new URI("https","example.com","/foo%2Fbar",null);
System.out.println(uri.toString());
var uri2 = new URI("https","example.com","/foo bar",null);
System.out.println(uri2.toString());
