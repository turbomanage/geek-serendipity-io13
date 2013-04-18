package com.google.cloud.backend.android.sample.geekwatch;

/**
 * Model object describing a geek user of this app
 *
 * @author David M. Chandler
 */
public class Geek {

	private String name = "No Name";
	private String interest = "I/O 13";
	private String geohash;

	public Geek(String name, String interest, String geohash) {
		this.name = name;
		this.interest = interest;
		this.geohash = geohash;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getInterest() {
		return interest;
	}
	public void setInterest(String interest) {
		this.interest = interest;
	}
	public String getGeohash() {
		return geohash;
	}
	public void setGeohash(String geohash) {
		this.geohash = geohash;
	}

}
