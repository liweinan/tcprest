package cn.huiwings.tcprest.protocol;

/**
 * Marker class representing null in V1 protocol.
 *
 * <p>V2 protocol uses string markers ("NULL") instead of this class.
 * This class is maintained for backward compatibility with V1 and
 * legacy code that may still reference it.
 *
 * @author Weinan Li
 * @date 08 01 2012
 * @deprecated V1 protocol specific. V2 uses "NULL" string marker directly instead of this class.
 *             Maintained for backward compatibility only.
 */
@Deprecated
public class NullObj {
}
