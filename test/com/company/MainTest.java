package com.company;

import org.junit.jupiter.api.Test;
import org.testng.Assert;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void repairUnicode2() {
        String name = "Eryk";
        Assert.assertEquals("Eryk", Main.repairString(name));
    }

    @Test
    void repairUnicode() {
        String name = "Pawe\\u00c5\\u0082";
        Assert.assertEquals("Paweł", Main.repairString(name));
    }
}