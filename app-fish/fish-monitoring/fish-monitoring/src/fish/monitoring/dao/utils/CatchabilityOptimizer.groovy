package fish.monitoring.dao.utils

import org.apache.commons.math3.analysis.MultivariateFunction
import org.apache.commons.math3.optim.InitialGuess
import org.apache.commons.math3.optim.MaxEval
import org.apache.commons.math3.optim.PointValuePair
import org.apache.commons.math3.optim.SimpleBounds
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer

class CatchabilityOptimizer {

    /**
     * Формула сигмоиды для расчета уловистости по возрасту
     */
    static double sigmoidQ(double age, double qMax, double a50, double k) {
        return qMax / (1.0d + Math.exp(-k * (age - a50)))
    }

    /**
     * Выполняет расчет оптимальных коэффициентов МНК (безпроизводный метод BOBYQA)
     *
     * @param catchesData Двумерный массив уловов: строки - даты, колонки - возраста
     * @param qKnown Массив известных коэффициентов q для каждой даты
     * @param initialGuess Начальное приближение [qMax, a50, k]. По умолчанию [0.6, 3.0, 1.0]
     * @param lowerBounds Нижние границы [qMax, a50, k]. По умолчанию [0.35, 0.5, 0.1]
     * @param upperBounds Верхние границы [qMax, a50, k]. По умолчанию [1.0, 10.0, 5.0]
     * @return Map с результатами: error (ошибка), qMax, a50, k, и qByAge (карта q для каждого возраста)
     */
    Map<String, Object> optimize(
            double[][] catchesData,
            double[] qKnown,
            double[] initialGuess = [0.60d, 3.0d, 1.0d] as double[],
            double[] lowerBounds  = [0.35d, 0.5d, 0.1d] as double[],
            double[] upperBounds  = [1.00d, 10.0d, 5.0d] as double[]
    ) {
        if (!catchesData || catchesData.length == 0 || !catchesData[0]) {
            throw new IllegalArgumentException("Массив уловов пуст или имеет неверный формат")
        }
        if (catchesData.length != qKnown.length) {
            throw new IllegalArgumentException("Количество дат в catchesData должно совпадать с длиной массива qKnown")
        }

        int numDates = catchesData.length
        int numAges = catchesData[0].length

        // Целевая функция МНК
        MultivariateFunction lossFunction = new MultivariateFunction() {
            @Override
            double value(double[] point) {
                double qMax = point[0]
                double a50  = point[1]
                double k    = point[2]

                double sumSquaredErrors = 0.0d

                for (int t = 0; t < numDates; t++) {
                    double totalCatch = 0.0d
                    double weightedQSum = 0.0d

                    for (int aIdx = 0; aIdx < numAges; aIdx++) {
                        double age = aIdx + 1 // Возраст (1, 2, 3...)
                        double c = catchesData[t][aIdx]

                        totalCatch += c
                        weightedQSum += c * sigmoidQ(age, qMax, a50, k)
                    }

                    // Защита от деления на ноль, если улов за дату равен нулю
                    double qCalculatedMean = totalCatch > 0 ? weightedQSum / totalCatch : 0.0d
                    double error = qCalculatedMean - qKnown[t]

                    sumSquaredErrors += error * error
                }

                return sumSquaredErrors
            }
        }

        // Оптимизатор BOBYQA
        // Количество точек адаптивной сетки = 2 * N + 1 = 2*3 + 1 = 7
        BOBYQAOptimizer optimizer = new BOBYQAOptimizer(7)

        PointValuePair result = optimizer.optimize(
                new MaxEval(10000),
                new ObjectiveFunction(lossFunction),
                GoalType.MINIMIZE,
                new InitialGuess(initialGuess),
                new SimpleBounds(lowerBounds, upperBounds)
        )

        double[] optimalParams = result.getPoint()
        double qMaxOpt = optimalParams[0]
        double a50Opt  = optimalParams[1]
        double kOpt    = optimalParams[2]

        // Рассчитываем точечные коэффициенты q(a) по возрастам
        Map<Integer, Double> qByAge = [:]
        for (int age = 1; age <= numAges; age++) {
            qByAge[age] = sigmoidQ(age, qMaxOpt, a50Opt, kOpt)
        }

        return [
                error : result.getValue(),
                qMax  : qMaxOpt,
                a50   : a50Opt,
                k     : kOpt,
                qByAge: qByAge
        ]
    }
}