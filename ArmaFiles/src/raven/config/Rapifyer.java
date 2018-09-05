package raven.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import raven.misc.ByteReader;
import raven.misc.TextReader;

public class Rapifyer {

	public static void main(String[] args) throws IOException, RapificationException, ConfigException {
		ByteReader bReader = new ByteReader(
				new FileInputStream(new File("/home/robert/Downloads/@cba/addons/accessoryUnpacked/config.bin")));

		ConfigClass rClass = ConfigClass.fromRapifiedFile(bReader);

		System.out.println(rClass);

		TextReader tReader = new TextReader(
				new FileInputStream(new File("/home/robert/Downloads/@cba/addons/accessoryUnpacked/config.cpp")));

		ConfigClass tClass = ConfigClass.fromTextFile(tReader);

		System.out.println(tClass);

		System.out.println("Equals: " + rClass.equals(tClass));

		bReader.close();
		tReader.close();
	}
}
