/**
 * 
 */
package org.bitcoinj.core;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mario
 *
 */
public class BlockchainAddResult {

	private Boolean success = Boolean.FALSE;

	private List<Block> orphansBlocksAdded = new ArrayList<Block>();
	private List<FilteredBlock> orphansFilteredBlocksAdded = new ArrayList<FilteredBlock>();



	public void addOrphan(Block block) {
		orphansBlocksAdded.add(block);
	}

	public void addOrphans(List<Block> blocks) {
		orphansBlocksAdded.addAll(blocks);
	}

	public void addFilteredOrphan(FilteredBlock block) {
		orphansFilteredBlocksAdded.add(block);
	}

	public void addFilteredOrphans(List<FilteredBlock> blocks) {
		orphansFilteredBlocksAdded.addAll(blocks);
	}

	public List<Block> getOrphansBlockAdded() {
		return orphansBlocksAdded;
	}
	
	public List<FilteredBlock> getFilteredOrphansAdded() {
		return orphansFilteredBlocksAdded;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public Boolean success() {
		return success;
	}

}
