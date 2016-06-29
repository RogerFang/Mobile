
package sort;

public final class StoreType
{
    public enum StoreUnit
    {
        GB,
        MB;
    }

    private StoreUnit unit;

    private int size;

    /**
     * @param unit
     * @param size
     */
    private StoreType(final StoreUnit unit, final int size)
    {
        super();
        this.unit = unit;
        this.size = size;
    }

    public static StoreType getInstance(final StoreUnit unit, final int size)
    {
        return new StoreType(unit, size);
    }

    public long getRealSize()
    {
        switch (unit)
        {
            case GB:
                return size * 1024 * 1024 * 1024;
            case MB:
                return size * 1024 * 1024;

        }
        return 0;
    }
}
