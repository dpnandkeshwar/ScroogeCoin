import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TxHandler {

	UTXOPool currPool;

	/*
	 * Creates a public ledger whose current UTXOPool (collection of unspent
	 * transaction outputs) is utxoPool. This should make a defensive copy of
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	public TxHandler(UTXOPool utxoPool) {
		currPool = new UTXOPool(utxoPool);
	}

	/*
	 * Returns true if 
	 * (1) all outputs claimed by tx are in the current UTXO pool,
	 * (2) the signatures on each input of tx are valid, 
	 * (3) no UTXO is claimed multiple times by tx, 
	 * (4) all of tx’s output values are non-negative, and 
	 * (5) the sum of tx’s input values is greater than or equal to the sum of its
	 * output values; and false otherwise.
	 */

	public boolean isValidTx(Transaction tx) {
		
		HashSet<UTXO> claimedCoins = new HashSet<UTXO>();
		double iSum = 0.0;
		double oSum = 0.0;
		
		for(int i = 0; i < tx.getInputs().size(); i++) {
			Transaction.Input currInput = tx.getInputs().get(i);
			UTXO utxo = new UTXO(currInput.prevTxHash, currInput.outputIndex);
			Transaction.Output currOutput = currPool.getTxOutput(utxo);
			
			if(currOutput == null)
				return false;
			
			iSum += currOutput.value;
			
			if(!currPool.contains(utxo))
				return false;
			
			RSAKey address = currOutput.address;
			if(address.verifySignature(tx.getRawDataToSign(i), currInput.signature) == false)
				return false;
			
			if(!claimedCoins.add(utxo))
				return false;
		}
		
		for(int i = 0; i < tx.getOutputs().size(); i++) {
			Transaction.Output currOutput = tx.getOutputs().get(i);
			oSum += currOutput.value;
			
			if(currOutput.value < 0)
				return false;
		}
		
		if(iSum < oSum)
			return false;


		return true;
	}

	/*
	 * Handles each epoch by receiving an unordered array of proposed transactions,
	 * checking each transaction for correctness, returning a mutually valid array
	 * of accepted transactions, and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		ArrayList<Transaction>  validTransactions = new ArrayList<Transaction>();
		
		for(int i = 0; i < possibleTxs.length; i++) {
			if(isValidTx(possibleTxs[i])) {
				validTransactions.add(possibleTxs[i]);
				
				ArrayList<Transaction.Input> inputs = possibleTxs[i].getInputs();
				
				for(int j = 0; j < inputs.size(); j++) {
					Transaction.Input currInput = inputs.get(j);
					UTXO consumedCoin = new UTXO(currInput.prevTxHash, currInput.outputIndex);
					currPool.removeUTXO(consumedCoin);
				}
				
				ArrayList<Transaction.Output> outputs = possibleTxs[i].getOutputs();
				
				for(int j = 0; j < outputs.size(); j++) {
					Transaction.Output currOutput = outputs.get(j);
					UTXO newCoin = new UTXO(possibleTxs[i].getHash(), j);
					currPool.addUTXO(newCoin, currOutput);
				}
			}
		}
		
		Transaction[] validTransactionArray = new Transaction[validTransactions.size()];
		return validTransactions.toArray(validTransactionArray);
	}
}
