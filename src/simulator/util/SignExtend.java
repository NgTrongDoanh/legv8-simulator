package simulator.util;

/**
 * Utility for sign extension.
 * (Giữ nguyên code từ phiên bản trước của bạn, đã khá tốt)
 */
public class SignExtend {
    private SignExtend() {} // Prevent instantiation

    public static long extend(int value, int originalBits) {
        if (originalBits <= 0 || originalBits > 32) {
            throw new IllegalArgumentException("originalBits for int input must be 1-32, was: " + originalBits);
        }
        if (originalBits == 32) return value;
        int signBitMask = 1 << (originalBits - 1);
        if ((value & signBitMask) != 0) { // Negative
            long extensionMask = (-1L) << originalBits;
            return value | extensionMask;
        } else { // Positive
            long lowerMask = (1L << originalBits) - 1;
            return (long)value & lowerMask;
        }
    }

    public static long extend(long value, int originalBits) {
        if (originalBits <= 0 || originalBits > 64) {
            throw new IllegalArgumentException("originalBits for long input must be 1-64, was: " + originalBits);
        }
        if (originalBits == 64) return value;
        long signBitMask = 1L << (originalBits - 1);
        long lowerMask = (1L << originalBits) - 1;
        if ((value & signBitMask) != 0) { // Negative
            long extensionMask = (-1L) << originalBits;
            return (value & lowerMask) | extensionMask;
        } else { // Positive
            return value & lowerMask;
        }
    }
     // Có thể thêm main() để test nếu muốn
}