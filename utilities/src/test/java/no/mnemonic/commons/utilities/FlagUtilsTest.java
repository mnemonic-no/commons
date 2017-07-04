package no.mnemonic.commons.utilities;


import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.Set;

import static no.mnemonic.commons.utilities.FlagUtils.*;
import static org.junit.Assert.*;

public class FlagUtilsTest {

  @Test
  public void testIsSet() throws Exception {
    assertTrue(isSet(0x22, 2));
    assertTrue(isSet(0x22L, 2L));
    assertFalse(isSet(0x20, 2));
    assertFalse(isSet(0x20L, 2L));
  }

  @Test
  public void testChangeBit() throws Exception {
    assertEquals(0x22, changeBit(0x20, 2, true));
    assertEquals(0x20, changeBit(0x20, 2, false));
    assertEquals(0x20, changeBit(0x22, 2, false));
    assertEquals(0x22, changeBit(0x22, 2, true));
    assertEquals(0x22, changeBit(0x20L, 2, true));
    assertEquals(0x20, changeBit(0x20L, 2, false));
    assertEquals(0x20, changeBit(0x22L, 2, false));
    assertEquals(0x22, changeBit(0x22L, 2, true));
  }

  @Test
  public void testSetBit() throws Exception {
    assertEquals(0x22, setBit(0x22, 2));
    assertEquals(0x22L, setBit(0x22L, 2L));
    assertEquals(0x22, setBit(0x20, 2));
    assertEquals(0x22L, setBit(0x20L, 2L));
    assertEquals(0x20, setBit(0x20, 0));
    assertEquals(0x20L, setBit(0x20L, 0L));
  }

  @Test
  public void testUnsetBit() throws Exception {
    assertEquals(0x20, unsetBit(0x20, 2));
    assertEquals(0x20L, unsetBit(0x20L, 2L));
    assertEquals(0x20, unsetBit(0x22, 2));
    assertEquals(0x20L, unsetBit(0x22L, 2L));
    assertEquals(0x22, unsetBit(0x22, 0));
    assertEquals(0x22L, unsetBit(0x22L, 0L));
  }

  @Test
  public void testOrBits() throws Exception {
    assertEquals(0x7L, or(ListUtils.list(1L,2L,4L)));
    assertEquals(0, or(ListUtils.list()));
    assertEquals(0, or(null));
  }

  @Test
  public void testSplit() throws Exception {
    assertEquals(SetUtils.set(), split(0));
    assertEquals(SetUtils.set(), split(0L));
    assertEquals(SetUtils.set(0x1,0x2,0x4), split(7));
    assertEquals(SetUtils.set(0x1L,0x2L,0x4L), split(7L));
  }

  @Test
  public void testProtect() throws Exception {
    assertEquals(2, protect(3, 1));
    assertEquals(3, protect(3, 0));
    assertEquals(0, protect(3, 3));
    assertEquals(0, protect(0, 3));
  }

  @Test
  public void testProtectedSet() throws Exception {
    assertEquals(2, protectedSet(2, 1, 1));
    assertEquals(3, protectedSet(2, 1, 0));
  }

  @Test
  public void testProtectedUnset() throws Exception {
    assertEquals(2, protectedUnset(2, 2, 2));
    assertEquals(0, protectedUnset(2, 2, 0));
    assertEquals(0, protectedUnset(2, 2, 1));
  }

  @Test
  public void testGetFlagBitPositions() throws Exception {
    long flag = or(Arrays.asList(0x1L, 0x2L, 0x8L));
    Set<Short> positions = getFlagBitPositions(flag);
    assertEquals(3, positions.size());
    assertTrue(positions.containsAll(Arrays.asList((short)0, (short)1, (short)3)));
  }

  @Test
  public void testSetFlagBitPositions() throws Exception {
    Set<Short> positions = SetUtils.set((short)0, (short)1, (short)3);
    long flag = setFlagByBitPositions(0L, positions);
    assertEquals(or(Arrays.asList(0x1L, 0x2L, 0x8L)), flag);
  }

  @Test
  public void testSetFlagBitPositionsWithOutRangeBit() throws Exception {
    Set<Short> positions = SetUtils.set((short)0, (short)64, (short)128);
    long flag = setFlagByBitPositions(0L, positions);
    assertEquals(1L, flag);
  }

  @Test
  public void testSetFlagBitPositionsWithExistingBit() throws Exception {
    Set<Short> positions = SetUtils.set((short) 1, (short) 3);
    long existFlag = FlagUtils.setFlagByBitPositions(0L, positions);
    long afterFlag = FlagUtils.setFlagByBitPositions(existFlag, positions);
    assertEquals(existFlag, afterFlag);
  }
}
