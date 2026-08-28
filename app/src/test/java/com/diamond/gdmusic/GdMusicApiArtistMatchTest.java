package com.diamond.gdmusic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GdMusicApiArtistMatchTest {

    @Test
    public void matchesWhenAtLeastOneChineseArtistIsShared() {
        assertTrue(GdMusicApi.hasMatchingArtist("周杰伦、温岚", "周杰伦"));
    }

    @Test
    public void matchesArtistsAcrossDifferentSeparatorsAndCase() {
        assertTrue(
                GdMusicApi.hasMatchingArtist(
                        "Taylor Swift / Ed Sheeran",
                        "taylor swift, Post Malone"
                )
        );
    }

    @Test
    public void rejectsSameTitleCandidateWithDifferentArtists() {
        assertFalse(GdMusicApi.hasMatchingArtist("周杰伦", "陈奕迅、林俊杰"));
    }
}
