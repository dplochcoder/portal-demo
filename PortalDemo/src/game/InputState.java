package game;

import java.util.Iterator;
import java.util.List;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class InputState {

  public abstract KeyState playerControlLeft();

  public abstract KeyState playerControlRight();

  public abstract KeyState playerControlDown();

  public abstract KeyState playerControlUp();

  public abstract KeyState playerControlCameraLeft();

  public abstract KeyState playerControlCameraRight();

  @Memoized
  public ImmutableList<KeyState> keyStateList() {
    return ImmutableList
        .of(
            playerControlLeft(),
            playerControlRight(),
            playerControlDown(),
            playerControlUp(),
            playerControlCameraLeft(),
            playerControlCameraRight());
  }

  public abstract Builder toBuilder();

  public static Builder builder() {
    return new AutoValue_InputState.Builder()
        .setPlayerControlLeft(KeyState.UNHELD)
        .setPlayerControlRight(KeyState.UNHELD)
        .setPlayerControlDown(KeyState.UNHELD)
        .setPlayerControlUp(KeyState.UNHELD)
        .setPlayerControlCameraLeft(KeyState.UNHELD)
        .setPlayerControlCameraRight(KeyState.UNHELD);
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setPlayerControlLeft(KeyState keyState);

    public abstract Builder setPlayerControlRight(KeyState keyState);

    public abstract Builder setPlayerControlDown(KeyState keyState);

    public abstract Builder setPlayerControlUp(KeyState keyState);

    public abstract Builder setPlayerControlCameraLeft(KeyState keyState);

    public abstract Builder setPlayerControlCameraRight(KeyState keyState);

    public Builder setKeyStateList(List<KeyState> keyStates) {
      Preconditions.checkArgument(keyStates.size() == 6);

      Iterator<KeyState> iter = keyStates.iterator();
      return setPlayerControlLeft(iter.next())
          .setPlayerControlRight(iter.next())
          .setPlayerControlDown(iter.next())
          .setPlayerControlUp(iter.next())
          .setPlayerControlCameraLeft(iter.next())
          .setPlayerControlCameraRight(iter.next());
    }

    public abstract InputState build();

  }

}
