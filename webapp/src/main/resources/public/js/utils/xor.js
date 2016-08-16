/**
 * xor of all the values
 * @param x
 * @param y
 * @param z
 * @return {boolean}
 */
export default function xor(x, y, z) {
    return !((x && y && z) || !(x || y || z));
}
