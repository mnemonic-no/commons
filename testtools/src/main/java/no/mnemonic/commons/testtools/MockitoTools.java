package no.mnemonic.commons.testtools;

import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.util.function.Predicate;

public class MockitoTools {

  /**
   * Simplify testing with mockito and argThat by doing
   * <p>
   * verify(mock).method(match(arg-&gt;arg.isSomething()));
   *
   * @param predicate predicate to match the expected argument
   * @param <T>       the argument type
   * @return a mock reporter which matches the argument using this predicate
   */
  public static <T> T match(Predicate<T> predicate) {
    return Mockito.argThat(new ArgMatcher<>(predicate));
  }

  /**
   * Same as {@link #match(Predicate)}, but with a specific expected argument class, to use if the verified
   * method takes a superclass of expected argument
   *
   * verify(mock).method(match(c-&gt;c.getValue()==expectedValue, ExpectedClass.class))
   * @param expectedClass Expected argument class
   * @param predicate predicate to test on the argument
   * @param <T> mocked method argument type
   * @param <U> expected argument type, subclass of T
   * @return true if argument is instanceof U and passes the predicate test
   */
  public static <T, U extends T> T match(Predicate<T> predicate, Class<U> expectedClass) {
    return Mockito.argThat(new ArgMatcher<>(o -> {
      if (!expectedClass.isInstance(o)) return false;
      //noinspection unchecked
      U target = (U) o;
      return predicate.test(target);
    }));
  }

  //helpers

  private static class ArgMatcher<T> implements ArgumentMatcher<T> {

    final Predicate<T> predicate;

    ArgMatcher(Predicate<T> predicate) {
      this.predicate = predicate;
    }

    @Override
    public boolean matches(T t) {
      return predicate.test(t);
    }
  }
}
