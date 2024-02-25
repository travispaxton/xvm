/**
 * A Matrix represents a fixed size, two-dimensional container of values. Most 2D coordinate systems
 * use `(x,y)` ordering, but by convention, matrices use `(row,col)`.
 *
 * TODO should 0-width and/or 0-height matrices be allowed?
 */
class Matrix<Element>
// TODO        implements Freezable
// TODO        implements Stringable
        incorporates conditional HashableMatrix<Element extends Hashable> {
        incorporates conditional FreezableMatrix<Element extends Shareable> {

    /**
     * Construct a fixed size matrix with the specified size and initial value. An initial value is
     * always required.
     *
     * @param rows    the number of rows
     * @param cols    the number of columns
     * @param supply  the value or the supply function for initializing the elements of the matrix
     */
    construct(Int rows, Int cols, Element | function Element(Int, Int) supply) {
        assert:bounds rows > 0 && cols > 0;
        this.rows  = rows;
        this.cols  = cols;
        this.cells = supply.is(Element)
                ? new Element[rows*cols](supply.as(Element)) // TODO GG should not need cast
                : new Element[rows*cols](TODO("translate i->row,col")); // TODO
    }

    /**
     * The height of the Matrix, which is the number of rows. The height of the matrix is fixed.
     */
    public/private Int rows;

    /**
     * The width of the Matrix, which is the number of columns. The width of the matrix is fixed.
     */
    public/private Int cols;

    /**
     * True iff the matrix is square.
     */
    Boolean square.get() {
        return rows == cols;
    }

    /**
     * True iff the matrix is symmetric.
     */
    Boolean symmetric.get() {
        if (!square) {
            return False;
        }

        for (Int row : 0..<rows) {
            for (Int col : row+1..<cols) {
// TODO GG                if (this[row,col] != this[col,row]) {
                if (this.getElement(row,col) != this.getElement(col,row)) {
                    return False;
                }
            }
        }

        return True;
    }

    /**
     * The contents of the Matrix. The form of the internal storage of elements is purposefully
     * hidden behind the API, so that the implementation of `Matrix` can be optimized by the
     * runtime in order to take advantage of hardware acceleration and optimized math libraries.
     */
    private Element[] cells;

    private Int index(Int row, Int col=0) {
        return row * cols + col;
    }

    /**
     * Obtain the value of the specified element of the matrix.
     *
     * @param row  a value in the range `0..<rows`
     * @param col  a value in the range `0..<cols`
     *
     * @return the Element at the specified coordinates
     */
    @Op("[]") Element getElement(Int row, Int col) {
        assert:bounds 0 <= row < rows && 0 <= col < cols;
        return cells[index(row, col)];
    }

    /**
     * Modify the value in the specified element of the matrix.
     *
     * @param row    a value in the range `0..<rows`
     * @param col    a value in the range `0..<cols`
     * @param value  the Element to store in the matrix at the specified coordinates
     */
    @Op("[]=") void setElement(Int col, Int row, Element value) {
        assert:bounds 0 <= row < rows && 0 <= col < cols;
        cells[index(row, col)] = value;
    }

    /**
     * Obtain a row vector from the matrix. The result _may_ be an unrealized slice, so use
     * [reify()](Array.reify) to ensure that the result will not have its contents modified by
     * subsequent modifications to this matrix.
     *
     * @param row  a value in the range `0..<rows`
     *
     * @return an `Element[]` representing the specified row
     */
    @Op("[?,_]") Element[] getRow(Int row) {
        assert:bounds 0 <= row < rows;
        return cells[index(row) ..< index(row+1)];
    }

    /**
     * Modify the specified row vector in the matrix.
     *
     * @param row     a value in the range `0..<rows`
     * @param vector  an `Element[]` of size `cols` representing the row to store
     */
    @Op("[?,_]=") void setRow(Int row, Element[] vector) {
        assert:bounds 0 <= row < rows && vector.size == cols;
        Int offset = index(row);
        for (Int col : 0..<cols) {
            cells[col+offset] = vector[col];
        }
    }

    /**
     * Obtain a column vector from the matrix. The result _may_ be an unrealized slice, so use
     * [reify()](Array.reify) to ensure that the result will not have its contents modified by
     * subsequent modifications to this matrix.
     *
     * @param col  a value in the range `0..<cols`
     *
     * @return an `Element[]` representing the specified column
     */
    @Op("[_,?]") Element[] getCol(Int col) {
        assert:bounds 0 <= col < cols;
// TODO GG return new Element[rows](row -> this[row,col]);
        return new Element[rows](row -> this.getElement(row,col));
    }

    /**
     * Modify the specified column vector in the matrix.
     *
     * @param col     a value in the range `0..<cols`
     * @param vector  an `Element[]` of size `rows` representing the column to store
     */
    @Op("[_,?]=") void setCol(Int col, Element[] vector) {
        assert:bounds 0 <= col < cols && vector.size == rows;
        Int index = col;
        for (Int row : 0..<rows) {
            cells[index] = vector[row];
            index += cols;
        }
    }

    /**
     * Returns a sub-matrix of this Matrix. The new Matrix will likely be backed by this Matrix,
     * which means that if this Matrix is mutable, changes made to this Matrix may be visible
     * through the new Matrix, and vice versa; if that behavior is not desired, {@link reify} the
     * value returned from this method.
     *
     * @param rowRange  the range of rows of this Matrix to obtain a slice for
     * @param colRange  the range of columns of this Matrix to obtain a slice for
     *
     * @return a slice of this Matrix corresponding to the specified ranges of columns and rows
     *
     * @throws OutOfBounds  if the specified ranges exceed either the lower or upper bounds of
     *                      the dimensions of this Matrix
     */
    @Op("[..]") Matrix slice(Range<Int> rowRange, Range<Int> colRange) {
        assert:bounds 0 <= colRange.effectiveLowerBound < colRange.effectiveUpperBound < cols;
        assert:bounds 0 <= rowRange.effectiveLowerBound < rowRange.effectiveUpperBound < rows;
// TODO GG return new Element[colRange.size, colRange.size](row,col->TODO("slice"));
        return new Matrix<Element>(rowRange.size, colRange.size,(row,col)->TODO("slice")); // TODO
    }

    /**
     * Obtain a Matrix of the same dimensions and that contains the same values as this Matrix, but
     * which has two additional attributes:
     *
     * * First, if this Matrix is a portion of a larger Matrix, then the returned Matrix will
     *   no longer be dependent on the larger Matrix for its storage;
     * * Second, if this Matrix is a portion of a larger Matrix, then changes to the returned
     *   Matrix will not be visible in the larger Matrix, and changes to the larger Matrix
     *   will not be visible in the returned Matrix.
     *
     * The contract is designed to allow for the use of copy-on-write and other lazy semantics to
     * achieve efficiency for both time and space.
     *
     * @return a reified Matrix
     */
    Matrix reify() {
        return this;
    }

    // ----- Comparable ----------------------------------------------------------------------------

    /**
     * Compare two arrays of the same type for equality.
     *
     * @return True iff the arrays have the same size, and for each index _i_, the element at that
     *         index from each array is equal
     */
    static <CompileType extends Matrix> Boolean equals(CompileType value1, CompileType value2) {
        Int rows = value1.rows;
        if (rows != value2.rows) {
            return False;
        }

        Int cols = value1.cols;
        if (cols != value2.cols) {
            return False;
        }

        for (Int row : 0 ..< rows) {
            for (Int col : 0 ..< cols) {
// TODO GG      if (value1[row, col] != value2[row, col]) {
                if (value1.getElement(row, col) != value2.getElement(row, col)) {
                    return False;
                }
            }
        }

        return True;
    }


    // ----- Freezable interface -------------------------------------------------------------------

    /**
     * Return an immutable array of the same type and contents as this array.
     *
     * All mutating calls to a `const` array will result in the creation of a new
     * `const` array with the requested changes incorporated.
     *
     * @param inPlace  pass True to indicate that the Array should make a frozen copy of itself if
     *                 it does not have to; the reason that making a copy is the default behavior is
     *                 to protect any object that already has a reference to the unfrozen array
     *
     * @throws Exception if any of the values in the array are not `service`, not `const`, and not
     *         {@link Freezable}
     */
    @Override
    immutable Array freeze(Boolean inPlace = False) {
        if (&this.isImmutable) {
            return this.as(immutable Array);
        }

        if (delegate.mutability == Constant) {
            // the underlying delegate is already frozen
            assert &delegate.isImmutable;
            mutability = Constant;
            return this.makeImmutable();
        }

        if (!inPlace) {
            return new Array(Constant, this).as(immutable Array);
        }

        // all elements must be immutable or Freezable (or exempt, i.e. a service); do not short
        // circuit this check, since we want to fail *before* we start freezing anything if the
        // array contains *any* non-freezable elements
        Boolean convert = False;
        for (Element element : this) {
            convert |= requiresFreeze(element);
        }

        if (convert) {
            loop: for (Element element : this) {
                if (Element+Freezable notYetFrozen := requiresFreeze(element)) {
                    this[loop.count] = notYetFrozen.freeze();
                }
            }
        }

        mutability = Constant;
        return makeImmutable();
    }


    // ----- Hashable mixin ------------------------------------------------------------------------

    private static mixin HashableMatrix<Element extends Hashable>
            into Matrix<Element>
            implements Hashable {
        /**
         * Calculate a hash code for this matrix.
         */
        static <CompileType extends HashableMatrix> Int hashCode(CompileType matrix) {
            // REVIEW: could include dimensions into the hash code
            return matrix.cells.hashCode();
        }
    }

// TODO numeric mixin
//    /**
//     * True iff the matrix is identity.
//     */
//    Boolean square.get() {
//        if (!square) {
//            return False;
//        }
//
//        for (Int i : 0..<rows) {
//            if (this[i,i] != 1) {
//                return False;
//            }
//        }
//
//        return True;
//    }
// TODO determinant
// TODO trace
}
