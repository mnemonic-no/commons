package no.mnemonic.commons.utilities;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class DeferredIteratorChainTest {

  @Test
  public void testAddOperation() {
    DeferredIteratorChain.Builder<Long> chain = DeferredIteratorChain.builder();
    assertDoesNotThrow(
        () -> chain.addOperation(() -> list(Instant.now().toEpochMilli()).iterator()));
    assertTrue(chain.build().hasNext());
  }

  @Test
  public void testIterateAndExecute() {
    DeferredIteratorChain.Builder<Long> chain = DeferredIteratorChain.builder();
    for (int i = 0; i < 10; i++) {
      chain.addOperation(
          () -> {
            try {
              Thread.sleep(2);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
            return list(Instant.now().toEpochMilli()).iterator();
          });
    }
    Long millisIteratorCreated = Instant.now().toEpochMilli();
    List<Long> moments = list();
    chain.build().forEachRemaining(moments::add);
    assertEquals(moments.size(), set(moments).size());
    List<Long> sortedCopy = moments.stream().sorted().toList();
    assertEquals(moments, sortedCopy);
    assertTrue(moments.get(0) > millisIteratorCreated);
    for (int i = 0; i < moments.size() - 1; i++) {
      assertTrue(moments.get(i + 1) > millisIteratorCreated);
      assertTrue(moments.get(i + 1) - moments.get(i) >= 2);
    }
  }

  @Test
  public void testEmptyChainHasNoNextAndNextThrows() {
    DeferredIteratorChain<Integer> chain = DeferredIteratorChain.<Integer>builder().build();
    assertFalse(chain.hasNext(), "Empty chain should have no next");

    NoSuchElementException ex =
        assertThrows(NoSuchElementException.class, chain::next);
    assertTrue(ex.getMessage().contains("No more elements"));
  }

  @Test
  public void testNullOperationRejected() {
    DeferredIteratorChain.Builder<String> b = DeferredIteratorChain.builder();
    NullPointerException npe = assertThrows(
        NullPointerException.class,
        () -> b.addOperation(null)
    );
    assertEquals("Operation must not be null", npe.getMessage());
  }

  @Test
  public void testSupplierReturningNullIsTreatedAsEmpty() {
    DeferredIteratorChain<String> chain =
        DeferredIteratorChain.<String>builder()
            .addOperation(() -> null)          // treated as empty
            .addOperation(Collections::<String>emptyIterator) // also empty
            .build();

    assertFalse(chain.hasNext());
  }
}

