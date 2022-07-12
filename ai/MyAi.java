package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	private static final int INFINITY = 1000;

	@Nonnull @Override public String name() { return "Lunatic AI"; }

	private Integer edgeValueWeight(Optional<ImmutableSet<ScotlandYard.Transport>> t) {  	//weight of various transport
		switch (t.toString()) {
			case "Taxi":
				return 1;
			case "Bus":
				return 2;
			case "Underground":
				return 4;
		}
		return null;
	}

	@Nonnull private int score(GameSetup setup, int source) {
		ArrayList<Integer> Unvisited = new ArrayList<>();
		ArrayList<Integer> Dist = new ArrayList<>();
		ArrayList<Integer> Prev = new ArrayList<>();

		for (int node : setup.graph.nodes()) {		//Initialization of Q
			Unvisited.add(node);
			Dist.add(INFINITY);
			Prev.add(0);
		}

		Dist.set(source, 0);

		while (!Unvisited.isEmpty()) {

			for (int adjacentNode : setup.graph.adjacentNodes(source)) {
				int alt = Dist.get(source) + edgeValueWeight(setup.graph.edgeValue(adjacentNode, source));
				if (alt < Dist.get(source)) {
					Dist.set(source, alt);
					adjacentNode.prev = sourceNode;
				}
			}
		}

		return sourceNode.dist;
	}

	private Move Minimax(Board board) {
		return null;
	}

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			@Nonnull AtomicBoolean terminate) {
		// returns a random move, replace with your own implementation
		var moves = board.getAvailableMoves().asList();
		return moves.get(new Random().nextInt(moves.size()));
	}
}
