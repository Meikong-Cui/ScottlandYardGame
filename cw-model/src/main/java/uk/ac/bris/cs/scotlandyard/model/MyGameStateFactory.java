package uk.ac.bris.cs.scotlandyard.model;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */

public final class MyGameStateFactory implements Factory<GameState> {

	@Override public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

	//MakeSingleMove Function
	private static ImmutableSet<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {

		final var singleMovesDetec = new ArrayList<SingleMove>();
		final var singleMovesMrx = new ArrayList<SingleMove>();

		for (int destination : setup.graph.adjacentNodes(source)) {
			var occupied = false;
			// Find out if destination is occupied by a detective
			for (final var p : detectives){
				if (destination == p.location()){
					occupied = true;
				}
			}
			if (occupied) continue;
			for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
				if (player.has(t.requiredTicket())) {
					singleMovesDetec.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
					singleMovesMrx.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
				}
			}
			// Add moves to the destination via a Secret ticket if there are any left with the player
			if (player.has(ScotlandYard.Ticket.SECRET)) {
				singleMovesMrx.add(new SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination));
			}
		}
		if (player.piece().isMrX()){
			return ImmutableSet.copyOf(singleMovesMrx);
		}
		else {
			return ImmutableSet.copyOf(singleMovesDetec);
		}
	}

	//MakeDoubleMove Function
	private static ImmutableSet<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
		final var doubleMoves = new ArrayList<DoubleMove>();
		if (!player.has(Ticket.DOUBLE) && setup.rounds.size() > 1) return ImmutableSet.of();
		for (SingleMove s : makeSingleMoves(setup, detectives, player, source)) {
				for (SingleMove s2 : makeSingleMoves(setup, detectives, player, s.destination)) {
					if (!player.hasAtLeast(s.ticket, 2) && s.ticket.equals(s2.ticket)) continue;
					doubleMoves.add(new DoubleMove(player.piece(), source, s.ticket, s.destination, s2.ticket, s2.destination));
				}
		}
		return ImmutableSet.copyOf(doubleMoves);

	}

	private static final class MyGameState implements GameState {

		//Constructors
		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableList<Player> everyone;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;

		private List<Player> updateDetectives(List<Player> detectives, Player player){
			List<Player> newDetectives = new ArrayList<>();
			for (final var d : detectives){
				if (d.piece() == player.piece()){
					newDetectives.add(player);
				}
				else {
					newDetectives.add(d);
				}
			}
			return newDetectives;
		}

		private MyGameState(final GameSetup setup,
							final ImmutableSet<Piece> remaining,
							final ImmutableList<LogEntry> log,
							final Player mrX,
							final List<Player> detectives) {

			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.everyone = combine();
			this.moves = getAvailableMoves();
			this.winner = getWinner();

			if (setup.rounds.isEmpty() || setup.graph.nodes().isEmpty()) throw new IllegalArgumentException();

			if (mrX.piece() == null || mrX.isDetective()) throw new IllegalArgumentException();

			for (final var p : detectives) {
				if (!p.isDetective()) {
					throw new IllegalArgumentException();
				}
				if (p.has(Ticket.DOUBLE) || p.has(Ticket.SECRET)) throw new IllegalArgumentException();
			}

			for (int i = 0; i < detectives.size(); i++) {
				for (int j = i + 1; j < detectives.size(); j++) {
					if (detectives.get(i).location() == detectives.get(j).location())
						throw new IllegalArgumentException();
				}
			}
		}

		//Methods
		//New method combine mrX and Detectives
		public ImmutableList<Player> combine() {
			List<Player> hold = new ArrayList<>();
			hold.add(mrX);
			hold.addAll(detectives);
			everyone = ImmutableList.copyOf(hold);
			return everyone;
		}

		//Change piece to player
		private Player changeType (Piece piece){
			for (final var e : everyone){
				if (e.piece() == piece){
					return e;
				}
			}
			return null;
		}

		//Set up the game
		@Override public GameSetup getSetup() { return setup; }

		//Find all players in a game
		@Override public ImmutableSet<Piece> getPlayers() {
			Set<Piece> players = new HashSet<>();
			players.add(mrX.piece());
			for (final var d : detectives) {
				players.add(d.piece());
			}
			return ImmutableSet.copyOf(players);
		}

		//Find detectives' location
		@Override public Optional<Integer> getDetectiveLocation(Detective detective) {
			for (final var p : detectives) {
				if (p.piece() == detective) return Optional.of(p.location());
			}
			return Optional.empty();
		}

		//Get mrX's travel log
		@Override public ImmutableList<LogEntry> getMrXTravelLog() { return log; }

		//Find the winner
		@Override public ImmutableSet<Piece> getWinner() {
			Set<Piece> win = new HashSet<>();
			int flag = 0;
			//Test if detectives are stuck
			for (Player d : detectives) {
				if (!d.has(ScotlandYard.Ticket.TAXI)
						&& !d.has(ScotlandYard.Ticket.BUS)
						&& !d.has(ScotlandYard.Ticket.UNDERGROUND)) {
					flag += 1;
				}
			}
			if (flag == detectives.size()) {
				win.add(mrX.piece());
				return ImmutableSet.copyOf(win);
			}
			//Test is mrX is captured
			for (Player p : detectives) {
				if (p.location() == mrX.location()) {
					for (final var x : detectives) {
						win.add(x.piece());
					}
					return ImmutableSet.copyOf(win);
				}
			}
			//All player have moved in a round
			//Test if mrX is stuck
			if (remaining.isEmpty()
					&& makeSingleMoves(setup, detectives, mrX, mrX.location()).isEmpty()){
				if (flag == detectives.size()) {
					win.add(mrX.piece());
					return ImmutableSet.copyOf(win);
				}
				else {
					for (Player x : detectives) {
						win.add(x.piece());
					}
					return ImmutableSet.copyOf(win);
				}
			}
			//Test if all rounds used
			if (log.size() == setup.rounds.size() && remaining.isEmpty()){
				win.add(mrX.piece());
				return ImmutableSet.copyOf(win);
			}
			return  ImmutableSet.of();
		}

		//Find each player's current ticket
		@Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			if (piece.isMrX()) {
				return Optional.of(ticket -> mrX.tickets().get(ticket));
			} else {
				for (final var p : detectives) {
					if (p.piece().equals(piece)) return Optional.of(ticket -> p.tickets().get(ticket));
				}
			}
			return Optional.empty();
		}

		//Find each player's available moves
		@Override public ImmutableSet<Move> getAvailableMoves() {
			Set<Move> current = new HashSet<>();
			Set<Piece> currentRemain = new HashSet<>(remaining);
			//Check if winner appears
			if (getWinner().isEmpty()) {
				//Check whether remaining is empty
				if (currentRemain.isEmpty()){
					currentRemain.add(mrX.piece());
				}
				//Add available moves for detectives
				for (Player p : detectives) {
					if (currentRemain.contains(p.piece())) {
						current.addAll(makeSingleMoves(setup, detectives, p, p.location()));
					}
				}
				//Add available moves for MrX
				if (currentRemain.contains(mrX.piece())) {
					current.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
					if (setup.rounds.size() > 1) {
						current.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
					}
				}
				//System.out.println("getWinner() is empty, game continues");
			}
			//System.out.println("getWinner() is not empty, game stops");
			return ImmutableSet.copyOf(current);
		}

		//Update the game state
		@Override public GameState advance(Move move) {
			Set<Piece> store = new HashSet<>(remaining);
			List<LogEntry> newLog = new ArrayList<>(log);

			if (store.isEmpty()){
				store.add(mrX.piece());
			}

			if (move.commencedBy().isMrX()) {
				for (final var p : detectives) {
					if (!makeSingleMoves(setup, detectives, p, p.location()).isEmpty()){
						store.add(p.piece());
					}
				}
				store.remove(mrX.piece());
			}
			else if (move.commencedBy().isDetective()){
				store.remove(move.commencedBy());
			}

			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);
			GameState newGameState = move.visit(new Visitor<GameState>() {
				@Override
				public GameState visit(SingleMove smove) {
					Player newX;
					List<Player> inRoundDetecs;
					//Update for log for mrX and find its current state
					if (smove.commencedBy().isMrX()) {
						newX = mrX.at(smove.destination).use(smove.ticket);
						if (setup.rounds.get(newLog.size())) {
							newLog.add(LogEntry.reveal(smove.ticket, smove.destination));
						} else {
							newLog.add(LogEntry.hidden(smove.ticket));
						}
						return new MyGameState(setup, ImmutableSet.copyOf(store), ImmutableList.copyOf(newLog), newX, detectives);
					}
					//Find new states for detectives
					if (smove.commencedBy().isDetective()) {
						Player p = changeType(smove.commencedBy());
						Player inRoundP = p.at(smove.destination).use(smove.ticket);
						newX = mrX.give(smove.ticket);
						inRoundDetecs = updateDetectives(detectives, inRoundP);
						return new MyGameState(setup, ImmutableSet.copyOf(store), ImmutableList.copyOf(newLog), newX, inRoundDetecs);
					}
					return new MyGameState(setup, ImmutableSet.copyOf(store), ImmutableList.copyOf(newLog), mrX, detectives);
				}

				@Override
				public GameState visit(DoubleMove dmove) {
					Player dNewX;
					if (setup.rounds.get(newLog.size())) {
						dNewX = mrX.at(dmove.destination1).use(dmove.ticket1);
						newLog.add(LogEntry.reveal(dmove.ticket1, dmove.destination1));
					}
					else {
						dNewX = mrX.at(dmove.destination1).use(dmove.ticket1);
						newLog.add(LogEntry.hidden(dmove.ticket1));
					}

					if (setup.rounds.get(newLog.size())) {
						dNewX = dNewX.at(dmove.destination2).use(dmove.ticket2);
						newLog.add(LogEntry.reveal(dmove.ticket2, dmove.destination2));
					}

					else {
						dNewX = dNewX.at(dmove.destination2).use(dmove.ticket2);
						newLog.add(LogEntry.hidden(dmove.ticket2));
					}
					dNewX = dNewX.use(Ticket.DOUBLE);
					return new MyGameState(setup, ImmutableSet.copyOf(store), ImmutableList.copyOf(newLog), dNewX, detectives);
				}
			});
			return newGameState;
		}
	}
}