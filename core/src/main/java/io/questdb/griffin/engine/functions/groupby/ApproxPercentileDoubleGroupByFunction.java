/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2019 Appsicle
 *  Copyright (c) 2019-2023 QuestDB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package io.questdb.griffin.engine.functions.groupby;

import io.questdb.cairo.ArrayColumnTypes;
import io.questdb.cairo.ColumnType;
import io.questdb.cairo.map.MapValue;
import io.questdb.cairo.sql.Function;
import io.questdb.cairo.sql.Record;
import io.questdb.cairo.sql.SymbolTableSource;
import io.questdb.griffin.PlanSink;
import io.questdb.griffin.SqlException;
import io.questdb.griffin.SqlExecutionContext;
import io.questdb.griffin.engine.functions.*;
import io.questdb.std.Numbers;
import io.questdb.std.ObjList;
import io.questdb.std.histogram.org.HdrHistogram.DoubleHistogram;

public class ApproxPercentileDoubleGroupByFunction extends DoubleFunction implements GroupByFunction, BinaryFunction {
    private final Function exprFunc;
    private final Function percentileFunc;
    // specifies the precision for the recorded values (between 0 and 5, defaults to 3).
    // trade-off between memory usage and accuracy.
    private final int precision;
    private final int funcPosition;
    private final ObjList<DoubleHistogram> histograms = new ObjList<>();
    private int histogramIndex;
    private int valueIndex;

    public ApproxPercentileDoubleGroupByFunction(Function exprFunc, Function percentileFunc, int precision, int funcPosition) {
        this.exprFunc = exprFunc;
        this.percentileFunc = percentileFunc;
        this.precision = precision;
        this.funcPosition = funcPosition;
    }

    @Override
    public void init(SymbolTableSource symbolTableSource, SqlExecutionContext executionContext) throws SqlException {
        BinaryFunction.super.init(symbolTableSource, executionContext);

        final double percentile = percentileFunc.getDouble(null);
        if (Double.isNaN(percentile) || percentile < 0 || percentile > 1) {
            throw SqlException.$(funcPosition, "percentile must be between 0 and 1");
        }
        if (precision < 0 || precision > 5) {
            throw SqlException.$(funcPosition, "precision must be between 0 and 5");
        }
    }

    @Override
    public void clear() {
        histograms.clear();
        histogramIndex = 0;
    }

    @Override
    public void computeFirst(MapValue mapValue, Record record) {
        final DoubleHistogram histogram;
        if (histograms.size() <= histogramIndex) {
            histograms.extendAndSet(histogramIndex, histogram = new DoubleHistogram(precision));
        } else {
            histogram = histograms.getQuick(histogramIndex);
        }
        histogram.reset();

        final double val = exprFunc.getDouble(record);
        if (Numbers.isFinite(val)) {
            histogram.recordValue(val);
        }
        mapValue.putLong(valueIndex, histogramIndex++);
    }

    @Override
    public void computeNext(MapValue mapValue, Record record) {
        final DoubleHistogram histogram = histograms.getQuick(mapValue.getInt(valueIndex));
        final double val = exprFunc.getDouble(record);
        if (Numbers.isFinite(val)) {
            histogram.recordValue(val);
        }
    }

    @Override
    public String getName() {
        return "approx_percentile";
    }

    @Override
    public Function getLeft() {
        return exprFunc;
    }

    @Override
    public Function getRight() {
        return percentileFunc;
    }

    @Override
    public double getDouble(Record rec) {
        if (histograms.size() == 0) {
            return Double.NaN;
        }

        final DoubleHistogram histogram = histograms.getQuick(rec.getInt(valueIndex));
        if (histogram.empty()) {
            return Double.NaN;
        }
        return histogram.getValueAtPercentile(percentileFunc.getDouble(null) * 100);
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public boolean isReadThreadSafe() {
        return false;
    }

    @Override
    public void pushValueTypes(ArrayColumnTypes columnTypes) {
        valueIndex = columnTypes.getColumnCount();
        columnTypes.add(ColumnType.LONG);
    }

    @Override
    public void setEmpty(MapValue mapValue) {
        mapValue.putLong(valueIndex, 0L);
    }

    @Override
    public void setNull(MapValue mapValue) {
        mapValue.putLong(valueIndex, Numbers.LONG_NaN);
    }
}