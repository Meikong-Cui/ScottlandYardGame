package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Board.*;
import uk.ac.bris.cs.scotlandyard.model.Model.Observer.*;

import java.util.*;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	@Nonnull @Override public Model build(GameSetup setup,
										  Player mrX,
										  ImmutableList<Player> detectives) {
		return new MyModel(setup, mrX, detectives);
	}

	private final class MyModel implements Model {
		private GameState modelState;
		private Set<Observer> observers;

		private MyModel(final GameSetup setup,
						final Player mrX,
						final List<Player> detectives) {
			this.observers = new HashSet<>();
			this.modelState = new MyGameStateFactory().build(setup, mrX, ImmutableList.copyOf(detectives));
		}

		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return modelState;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
			if (observer.equals(null)) throw new IllegalArgumentException();
			if (!observers.contains(observer)) {
				observers.add(observer);
			}
			else throw new IllegalArgumentException();
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			if (observer.equals(null)) throw new IllegalArgumentException();
			if (observers.contains(observer)) {
				observers.remove(observer);
			}
			else throw new IllegalArgumentException();
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return ImmutableSet.copyOf(observers);
		}

		@Override public void chooseMove(@Nonnull Move move) {
			modelState = modelState.advance(move);
			var event = modelState.getWinner().isEmpty() ? Event.MOVE_MADE : Event.GAME_OVER;
			for (Observer o : observers) o.onModelChanged(modelState, event);
		}
	}
}
