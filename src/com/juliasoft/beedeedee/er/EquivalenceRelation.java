package com.juliasoft.beedeedee.er;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.juliasoft.beedeedee.bdd.Assignment;

/**
 * A set of equivalence classes. This is an immutable class
 * and can be shared safely.
 */

public class EquivalenceRelation implements Iterable<BitSet> {
	private final BitSet[] equivalenceClasses;
	private final static BitSet[] empty = new BitSet[0];

	private EquivalenceRelation(List<BitSet> equivalenceClasses) {
		this.equivalenceClasses = new BitSet[equivalenceClasses.size()];
		int pos = 0;
		for (BitSet eqClass: equivalenceClasses)
			this.equivalenceClasses[pos++] = eqClass;
	}

	private EquivalenceRelation(BitSet[] equivalenceClasses) {
		this.equivalenceClasses = equivalenceClasses;
	}

	public EquivalenceRelation() {
		this.equivalenceClasses = empty;
	}

	public EquivalenceRelation(EquivalenceRelation parent, Filter filter) {
		this(filterClasses(parent, filter));
	}

	private static List<BitSet> filterClasses(EquivalenceRelation parent, Filter filter) {
		List<BitSet> equivalenceClasses = new ArrayList<>();
		for (BitSet eqClass: parent)
			if (filter.accept(eqClass))
				equivalenceClasses.add(eqClass);

		return equivalenceClasses;
	}

	public EquivalenceRelation(int[][] classes) {
		this.equivalenceClasses = new BitSet[classes.length];

		for (int pos = 0; pos < classes.length; pos++) {
			BitSet added = new BitSet();
			for (int i: classes[pos])
				added.set(i);

			equivalenceClasses[pos] = added;
		}
	}

	/**
	 * Computes the intersection of two E's.
	 * 
	 * @param other the other set
	 * @return the resulting set
	 */

	public EquivalenceRelation intersection(EquivalenceRelation other) {
		List<BitSet> intersection = new ArrayList<>();
		for (BitSet set1: equivalenceClasses) {
			for (BitSet set2: other.equivalenceClasses) {
				BitSet element = (BitSet) set1.clone();
				element.and(set2);
				if (element.cardinality() > 1)
					intersection.add(element);
			}
		}

		return new EquivalenceRelation(intersection);
	}

	public boolean isEmpty() {
		return equivalenceClasses.length == 0;
	}

	public int size() {
		return equivalenceClasses.length;
	}

	@Override
	public Iterator<BitSet> iterator() {
		return Arrays.asList((BitSet[]) equivalenceClasses).iterator();
	}

	/**
	 * Generates a list containing all pairs of equivalent variables
	 * 
	 * @return the list of pairs
	 */
	public List<Pair> pairs() {
		ArrayList<Pair> pairs = new ArrayList<>();
		for (BitSet bs: equivalenceClasses)
			for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
				for (int j = bs.nextSetBit(i + 1); j >= 0; j = bs.nextSetBit(j + 1))
					pairs.add(new Pair(i, j));

		return pairs;
	}

	/**
	 * Subtracts the pairs in the other set from this set.
	 * 
	 * @param other the other set
	 * @return the list of the pairs of this set not contained in the other
	 */
	public List<Pair> pairsInDifference(EquivalenceRelation other) {
		List<Pair> myPairs = pairs();
		List<Pair> otherPairs = other.pairs();

		BitSet toRemove = new BitSet();
		for (Pair pair: myPairs)
			if (otherPairs.contains(pair))
				toRemove.set(pair.second);

		List<Pair> result = new ArrayList<>();
		for (Pair pair: myPairs)
			if (!toRemove.get(pair.first) && !toRemove.get(pair.second))
				result.add(pair);

		return result;
	}

	/**
	 * Adds pairs to this set.
	 * 
	 * @param pairs the pairs to add
	 */
	public EquivalenceRelation addPairs(Iterable<Pair> pairs) {
		List<BitSet> newEquivalenceClasses = new ArrayList<>();
		for (BitSet eqClass: equivalenceClasses)
			newEquivalenceClasses.add(eqClass);

		for (Pair pair: pairs)
			addPair(pair, newEquivalenceClasses);

		return new EquivalenceRelation(newEquivalenceClasses);
	}

	/**
	 * Adds a pair to this set.
	 * 
	 * @param pair the pair to add
	 */
	private static void addPair(Pair pair, List<BitSet> where) {
		int pos1 = findClass(pair.first, where);
		int pos2 = findClass(pair.second, where);

		if (pos1 >= 0) {
			if (pos2 >= 0) {
				BitSet c1 = where.get(pos1);
				BitSet c2 = where.get(pos2);

				if (!c1.equals(c2)) {
					c1 = (BitSet) c1.clone();
					c1.or(c2);
					where.set(pos1, c1);
					where.remove(pos2);
				}
			}
			else {
				BitSet c1 = (BitSet) where.get(pos1).clone();
				c1.set(pair.second);
				where.set(pos1, c1);
			}
		}
		else
			if (pos2 >= 0) {
				BitSet c2 = (BitSet) where.get(pos2).clone();
				c2.set(pair.first);
				where.set(pos2, c2);
			}
			else {
				BitSet eqClass = new BitSet();
				eqClass.set(pair.first);
				eqClass.set(pair.second);
				where.add(eqClass);
			}
	}

	public EquivalenceRelation addClasses(EquivalenceRelation other) {
		List<BitSet> newEquivalenceClasses = new ArrayList<>();
		for (BitSet eqClass: equivalenceClasses)
			newEquivalenceClasses.add(eqClass);

		for (BitSet added: other.equivalenceClasses)
			addClass(added, newEquivalenceClasses);

		return new EquivalenceRelation(newEquivalenceClasses);
	}

	private static void addClass(BitSet added, List<BitSet> where) {
		BitSet intersected = null;
		LinkedList<Integer> toRemove = new LinkedList<>();

		for (int pos = 0; pos < where.size(); pos++) {
			BitSet cursor = where.get(pos);

			if (cursor.intersects(added)) {
				if (intersected == null) {
					where.set(pos, intersected = (BitSet) cursor.clone());
					intersected.or(added);
				}
				else {
					intersected.or(cursor);
					toRemove.addFirst(pos);
				}
			}
		}

		if (intersected == null)
			where.add((BitSet) added.clone());
		else
			for (Integer pos: toRemove)
				where.remove((int) pos);
	}

	private BitSet findClass(int n) {
		for (BitSet eqClass: equivalenceClasses)
			if (eqClass.get(n))
				return eqClass;

		return null;
	}

	private int findIndexOfClass(int n) {
		for (int pos = 0; pos < equivalenceClasses.length; pos++) {
			BitSet eqClass = equivalenceClasses[pos];
			if (eqClass.get(n))
				return pos;
		}

		return -1;
	}

	private static int findClass(int n, List<BitSet> where) {
		int pos = 0;
		for (BitSet eqClass: where) {
			if (eqClass.get(n))
				return pos;

			pos++;
		}

		return -1;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof EquivalenceRelation &&
			Arrays.deepEquals(((EquivalenceRelation) obj).equivalenceClasses, equivalenceClasses);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(equivalenceClasses);
	}

	@Override
	public String toString() {
		return equivalenceClasses.toString();
	}

	public int maxVar() {
		int max = 0;
		for (BitSet eqClass: equivalenceClasses)
			max = Math.max(max, eqClass.length() - 1);

		return max;
	}

	/**
	 * Updates the given assignment with information on equivalent variables.
	 * 
	 * @param a the assignment to update
	 */
	void updateAssignment(Assignment a) {
		classIteration:
		for (BitSet eqClass: equivalenceClasses) {
			for (int i = eqClass.nextSetBit(0); i >= 0; i = eqClass.nextSetBit(i + 1)) {
				try {
					if (a.holds(i)) {
						setAll(a, eqClass, true);
						continue classIteration;
					}
				} catch (Exception e) {
					// ignore exception if variable not in assignment
				}
			}
			setAll(a, eqClass, false);
		}
	}

	private static void setAll(Assignment a, BitSet eqClass, boolean value) {
		for (int i = eqClass.nextSetBit(0); i >= 0; i = eqClass.nextSetBit(i + 1))
			a.put(i, value);
	}

	public EquivalenceRelation removeVar(int var) {
		int pos = findIndexOfClass(var);
		if (pos >= 0) {
			BitSet c = equivalenceClasses[pos];
			if (c.cardinality() > 2) {
				BitSet[] newEquivalenceClasses = equivalenceClasses.clone();
				
				BitSet eqClass = (BitSet) c.clone();
				eqClass.clear(var);
				newEquivalenceClasses[pos] = eqClass;

				return new EquivalenceRelation(newEquivalenceClasses);
			}
			else {
				List<BitSet> newEquivalenceClasses = new ArrayList<>();
				for (int i = 0; i < equivalenceClasses.length; i++)
					if (i != pos)
						newEquivalenceClasses.add(equivalenceClasses[i]);

				return new EquivalenceRelation(newEquivalenceClasses);
			}
		}
		else
			return this;
	}

	public int nextLeader(int var) {
		return findClass(var).nextSetBit(var + 1);
	}

	public int nextLeader(int var, BitSet excludedVars) {
		BitSet c = findClass(var);
		int leader = c.nextSetBit(0);
		while (excludedVars.get(leader) || leader == var) {
			leader = c.nextSetBit(leader + 1);
			if (leader < 0)
				return -1;
		}
		return leader;
	}

	public EquivalenceRelation replace(Map<Integer, Integer> renaming) {
		BitSet[] where = null;

		for (Integer i: renaming.keySet()) {
			int pos = findIndexOfClass(i);
			if (pos >= 0) {
				if (where == null)
					where = equivalenceClasses.clone();

				BitSet c = where[pos];
				c = (BitSet) c.clone();
				c.clear(i);
				c.set(renaming.get(i));
				where[pos] = c;
			}
		}

		if (where == null)
			return this;
		else
			return new EquivalenceRelation(where);
	}

	public boolean containsVar(int var) {
		for (BitSet eqClass: equivalenceClasses)
			if (eqClass.get(var))
				return true;

		return false;
	}

	public int getLeader(int var) {
		for (BitSet eqClass: equivalenceClasses)
			if (eqClass.get(var))
				return eqClass.nextSetBit(0);

		return var;
	}

	public int getLeader(int var, Filter filter) {
		for (BitSet eqClass: equivalenceClasses)
			if (eqClass.get(var) && filter.accept(eqClass))
				return eqClass.nextSetBit(0);

		return -1;
	}

	/**
	 * Finds the minimum leader that is greater or equal to c
	 * 
	 * @param c
	 * @return the minimum leader >= c, or -1 if it does not exist
	 */
	public int getMinLeaderGreaterOrEqualtTo(int c, int var, Filter filter) {
		int min = -1;
		for (BitSet eqClass: equivalenceClasses) {
			int leader = eqClass.nextSetBit(0);
			if (leader >= c && (min < 0 || leader < min) && leader < var && filter.accept(eqClass))
				min = leader;
		}
	
		return min;
	}

	public static interface Filter {
		public boolean accept(BitSet eqClass);
	}
}