package com.juliasoft.beedeedee.factories;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

public class ResizingAndGarbageCollectedUniqueTableTest {

	private ResizingAndGarbageCollectedUniqueTable ut;
	private Factory factoryMock;

	@Before
	public void setUp() {
		factoryMock = mock(Factory.class);
		ut = new ResizingAndGarbageCollectedUniqueTable(10, 10, factoryMock);
	}

	@Test
	public void testCompactTable1() {
		// terminals
		ut.get(Integer.MAX_VALUE - 1, -1, -1);
		ut.get(Integer.MAX_VALUE, -1, -1);

		assertEquals(2, ut.get(3, 0, 1));
		assertEquals(3, ut.get(4, 0, 1));

		// let's kill node 2
		boolean[] aliveNodes = new boolean[] { true, true, false, true };

		// so compactTable collects it
		assertEquals(1, ut.compactTable(aliveNodes));
		Arrays.fill(ut.H, -1);
		ut.updateHashTable();

		// x4 should have been shifted back by one
		assertEquals(2, ut.get(4, 0, 1));
		assertEquals(3, ut.nextPos);
	}

	@Test
	public void testCompactTable2() {
		// terminals
		ut.get(Integer.MAX_VALUE - 1, -1, -1);
		ut.get(Integer.MAX_VALUE, -1, -1);

		assertEquals(2, ut.get(31, 0, 1));
		assertEquals(3, ut.get(4, 0, 1));
		assertEquals(4, ut.get(25, 3, 1));

		// let's kill node 2
		boolean[] aliveNodes = new boolean[] { true, true, false, true, true };

		// so compactTable collects it
		assertEquals(1, ut.compactTable(aliveNodes));
		Arrays.fill(ut.H, -1);
		ut.updateHashTable();

		// x4 should have been shifted back by one
		assertEquals(2, ut.get(4, 0, 1));
		// reference in other node changed as well
		assertEquals(4, ut.nextPos);
		assertEquals(3, ut.get(25, 2, 1));	// was 25, 3, 1
	}

	@Test
	public void testUpdateHashTable1() {
		ut.get(3, 0, 1);
		ut.get(4, 10, 1);
		ut.get(5, 100, 0);

		int[] H = ut.H;

		Arrays.fill(H, -1);
		ut.updateHashTable();

		// H contains node position, given its hash
		assertEquals(0, H[ut.hash(3, 0, 1)]);
		assertEquals(1, H[ut.hash(4, 10, 1)]);
		assertEquals(2, H[ut.hash(5, 100, 0)]);
	}

	@Test
	public void testUpdateHashTable2() {
		redefineHashInTestDouble(10);
		ut.get(3, 0, 1);
		// hash(...) considers only low and high, so these should have the same hash (same chain)
		ut.get(4, 10, 1);
		ut.get(5, 10, 1);

		assertEquals(2, ut.next(1));
		// reset next for node 1
		ut.setNext(1, -1);

		Arrays.fill(ut.H, -1);
		ut.updateHashTable();

		// should be restored by updateHashTable
		assertEquals(2, ut.next(1));
	}

	/**
	 * Redefine hash to use only low and high.
	 */
	private void redefineHashInTestDouble(int size) {
		ut = new ResizingAndGarbageCollectedUniqueTable(size, 10, factoryMock) {
			@Override
			protected int hash(int var, int low, int high, int size) {
				int num = low;

				while (low > 0) {
					low >>>= 1;
					high <<= 1;
				}

				return (num |= high) >= 0 ? num % size : (-num) % size;
			}
		};
	}

	@Test
	public void testExpandTable() {
		int pos = ut.hash(13, 20, 41);
		int createdNode = ut.expandTable(13, 20, 41, null, pos );

		// node is actually created by expandTable
		assertEquals(createdNode, ut.H[pos]);
		assertEquals(13, ut.var(createdNode));
		assertEquals(20, ut.low(createdNode));
		assertEquals(41, ut.high(createdNode));
	}

	@Test
	public void testExpandTableTwoNodesSameHash() {
		int pos = ut.hash(13, 20, 41);
		int node1 = ut.expandTable(13, 20, 41, null, pos);
		int node2 = ut.expandTable(1024, 20, 41, null, pos);

		assertEquals(node2, ut.next(node1));
	}

	@Test
	public void testExpandTableDebugging() {
		redefineHashInTestDouble(2);
		ut.get(1, 2, 3);
		ut.get(2, 2, 3);
		// this triggers resizing, hash changes in the middle of operation
		int pos1 = ut.get(3, 3, 2);
		int pos2 = ut.get(3, 3, 2);
		assertEquals(pos1, pos2);
	}
}
