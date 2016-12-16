package no.mnemonic.commons.utilities;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Various bit operations on int/long flag bits.
 */
public class FlagUtils {

  /**
   * @param flags          flags to set
   * @param protectedFlags bits to leave unset
   * @return Returns the flags, but with protected bits unset
   */
  public static long protect(long flags, long protectedFlags) {
    return (flags & ~protectedFlags);
  }

  /**
   * Sets the setFlags in oldFlags, but leaves all the protected bits untouched
   *
   * @param oldFlags       existing flags
   * @param setFlags       flag bits to set
   * @param protectedFlags all bits which should be protected
   * @return oldFlags ORed with setFlags, except for protected bits, which are returned as set in oldFlags
   */
  public static long protectedSet(long oldFlags, long setFlags, long protectedFlags) {
    return setBit(oldFlags, protect(setFlags, protectedFlags));
  }

  /**
   * Unsets the unsetFlags in oldFlags, but leaves all the protected bits untouched
   *
   * @param oldFlags       existing flags
   * @param unsetFlags     flag bits to set
   * @param protectedFlags all bits which should be protected
   * @return oldFlags ANDed with setFlags, except for protected bits, which are returned as set in oldFlags
   */
  public static long protectedUnset(long oldFlags, long unsetFlags, long protectedFlags) {
    return unsetBit(oldFlags, protect(unsetFlags, protectedFlags));
  }

  /**
   * Check if a bit or bit combination is set
   * @param flags bitmap to test
   * @param flag flags to check
   * @return true if all bits in flag are set in flags
   */
  public static boolean isSet(long flags, long flag) {
    return (flags & flag) == flag;
  }

  /**
   * Check if a bit or bit combination is set
   * @param flags bitmap to test
   * @param flag bits to check
   * @return true if all bits in flag are set in flags
   */
  public static boolean isSet(int flags, int flag) {
    return (flags & flag) == flag;
  }

  /**
   * Unset a bit or bit combination from bitmap
   * @param flags  bitmap to change
   * @param bit bits to unset
   * @return updated bitmap with specified bits cleared
   */
  public static long unsetBit(long flags, long bit) {
    return flags & ~bit;
  }

  /**
   * Unset a bit or bit combination from bitmap
   * @param flags  bitmap to change
   * @param bit bits to unset
   * @return updated bitmap with specified bits cleared
   */
  public static int unsetBit(int flags, int bit) {
    return flags & ~bit;
  }

  /**
   * Set a bit or bit combination on bitmap
   * @param flags  bitmap to change
   * @param bit bits to set
   * @return updated bitmap with specified bits set
   */
  public static int setBit(int flags, int bit) {
    return flags | bit;
  }

  /**
   * Set a bit or bit combination on bitmap
   * @param flags  bitmap to change
   * @param bit bits to set
   * @return updated bitmap with specified bits set
   */
  public static long setBit(long flags, long bit) {
    return flags | bit;
  }

  /**
   * Set or unset a bit or bit combination on bitmap
   * @param flags  bitmap to change
   * @param bit bits to change
   * @param set if true, set the bits, if not unset them
   * @return updated bitmap with specified bits updated
   * @see {@link #unsetBit(int, int)} and {@link #setBit(int, int)}
   */
  public static int changeBit(int flags, int bit, boolean set) {
    if (set) return setBit(flags, bit);
    else return unsetBit(flags, bit);
  }

  /**
   * Set or unset a bit or bit combination on bitmap
   * @param flags  bitmap to change
   * @param bit bits to change
   * @param set if true, set the bits, if not unset them
   * @return updated bitmap with specified bits updated
   * @see {@link #unsetBit(long, long)} and {@link #setBit(long, long)}
   */
  public static long changeBit(long flags, long bit, boolean set) {
    if (set) return setBit(flags, bit);
    else return unsetBit(flags, bit);
  }

  /**
   * Combine a set of bitmaps
   * @param flags set of bitmaps
   * @return a bitmap created from ORing together all the bitmaps
   */
  public static long or(Collection<Long> flags) {
    if (flags == null) return 0;
    long result = 0;
    for (Long l : flags) {
      if (l == null) continue;
      result |= l;
    }
    return result;
  }

  /**
   * Split bit flags into a set of single bits
   * @param flags long with bits to split into flags
   * @return a set of single bits, one for each set bit
   */
  public static Set<Long> split(long flags) {
    Set<Long> l = new HashSet<>();
    for (int i = 0; i < 64; i++) {
      if (((flags >> i) & 1L) == 1L) l.add(1L << i);
    }
    return l;
  }

  /**
   * Split bit flags into a set of single bits
   * @param flags int with bits to split into flags
   * @return a set of single bits, one for each set bit
   */
  public static Set<Integer> split(int flags) {
    Set<Integer> l = new HashSet<>();
    for (int i = 0; i < 32; i++) {
      if (((flags >> i) & 1L) == 1L) l.add(1 << i);
    }
    return l;
  }

  /**
   * Find out for long flag value which bit positions has been assigned 1
   * @param flag long flag
   * @return set of bit positions which have values assigned
   */
  public static Set<Short> getFlagBitPositions(long flag) {
    Set<Short> positions = new HashSet<>();
    for (short i = 0; i < 64; i ++) {
      if ((flag & (1L << i)) != 0) {
        // bit assigned in position i
        positions.add(i);
      }
    }
    return positions;
  }

  /**
   * Construct flag base on initial flag and bit positions to be assigned with 1
   * @param initFlag initial flag
   * @param positions set of bit positions of flag suppose to be assigned with 1
   * @return new flag after assigned 1 to bit positions on initial flag
   */
  public static long setFlagByBitPositions(long initFlag, Set<Short> positions) {
    long flag = initFlag;
    if (positions == null || positions.isEmpty()) {
      return flag;
    }
    for (Short pos : positions) {
      if (pos == null) {
        continue;
      }
      if (pos < 0 || pos > 63) {
        // ignore flag position which out of range (for long 0 ~ 63)
        continue;
      }
      flag += 1L << pos;
    }
    return flag;
  }
}
