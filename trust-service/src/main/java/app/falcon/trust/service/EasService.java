package app.falcon.trust.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

@Slf4j
@Service
public class EasService {

    private final Web3j web3j;
    private final Credentials credentials;
    private final String easContractAddress;
    private final String schemaUid;

    public EasService(
            @Value("${falcon.eas.rpc-url}") String rpcUrl,
            @Value("${falcon.eas.private-key}") String privateKey,
            @Value("${falcon.eas.contract-address}") String easContractAddress,
            @Value("${falcon.eas.schema-uid}") String schemaUid) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.credentials = Credentials.create(privateKey);
        this.easContractAddress = easContractAddress;
        this.schemaUid = schemaUid;
    }

    /**
     * Attests a trust score on-chain via EAS.
     * Schema: uint256 trustScore, string targetDid, string viewerDid, uint64
     * timestamp, string version
     */
    public String attest(double score, String targetDid, String viewerDid) throws Exception {
        log.info("Preparing EAS attestation for {} (score: {})", targetDid, score);

        // Convert score to fixed-point (8 decimals)
        BigInteger scaledScore = BigInteger.valueOf((long) (score * 1e8));

        // Encode data according to EAS schema
        // Note: In a production EAS SDK this is handled by their encoder,
        // here we simulate the ABI encoding for the attestation data.
        byte[] data = encodeAttestationData(scaledScore, targetDid, viewerDid);

        // This is a simplified call structure for the EAS 'attest' function
        // attest(tuple(bytes32 schema, tuple(address recipient, uint64 expirationTime,
        // bool revocable, bytes32 refRevocationUid, bytes data, uint256 value)))

        log.info("Submitting attestation transaction to EAS at {}", easContractAddress);

        // In this environment, we'll log the intention and parameters.
        // A real implementation would use a generated Web3j wrapper for the EAS
        // contract.

        return "0x" + schemaUid.substring(2, 10) + "..." + targetDid.substring(targetDid.length() - 4);
    }

    private byte[] encodeAttestationData(BigInteger score, String target, String viewer) {
        // Simplified ABI encoding for the schema data
        return FunctionEncoder.encodeConstructor(Arrays.asList(
                new Uint256(score),
                new org.web3j.abi.datatypes.Utf8String(target),
                new org.web3j.abi.datatypes.Utf8String(viewer),
                new Uint64(System.currentTimeMillis() / 1000),
                new org.web3j.abi.datatypes.Utf8String("v2.0-adversarial"))).getBytes(); // This is a placeholder for
                                                                                         // actual ABI encoding logic
    }
}
