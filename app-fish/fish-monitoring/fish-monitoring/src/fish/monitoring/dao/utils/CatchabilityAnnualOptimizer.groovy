package fish.monitoring.dao.utils

import org.apache.commons.math3.analysis.MultivariateFunction
import org.apache.commons.math3.optim.InitialGuess
import org.apache.commons.math3.optim.MaxEval
import org.apache.commons.math3.optim.PointValuePair
import org.apache.commons.math3.optim.SimpleBounds
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer

class CatchabilityAnnualOptimizer {

    /**
     * Формула сигмоиды для расчета уловистости по возрасту
     */
    static double sigmoidQ(double age, double qMax, double a50, double k) {
        return qMax / (1.0d + Math.exp(-k * (age - a50)))
    }

    /**
     * Выполняет расчет оптимальных коэффициентов МНК на основе единого годового qKnown
     *
     * @param catchesData Двумерный массив уловов: строки - даты, колонки - возраста
     * @param qKnown Известное среднегодовое значение q для вида
     * @param customInitialGuess Пользовательское начальное приближение (опционально)
     * @param customLowerBounds Пользовательские нижние границы (опционально)
     * @param customUpperBounds Пользовательские верхние границы (опционально)
     * @return Map с результатами: error (квадрат ошибки), qMax, a50, k, и qByAge (карта q для каждого возраста)
     */
    Map<String, Object> optimize(
            double[][] catchesData,
            double qKnown,
            double[] customInitialGuess = null,
            double[] customLowerBounds = null,
            double[] customUpperBounds = null
    ) {
        if (!catchesData || catchesData.length == 0 || !catchesData[0]) {
            throw new IllegalArgumentException("Массив уловов пуст или имеет неверный формат")
        }

        int numDates = catchesData.length
        int numAges = catchesData[0].length

        // Динамические значения по умолчанию базируются на qKnown (если не переданы кастомные)
        double[] lowerBounds = customLowerBounds ?: [qKnown, 0.5d, 0.1d] as double[]
        double[] upperBounds = customUpperBounds ?: [1.00d, 12.0d, 5.0d] as double[]
        double[] initialGuess = customInitialGuess ?: [Math.min(qKnown * 1.5d, 0.9d), 3.0d, 1.0d] as double[]

        // Целевая функция МНК
        MultivariateFunction lossFunction = new MultivariateFunction() {
            @Override
            double value(double[] point) {
                double qMax = point[0]
                double a50  = point[1]
                double k    = point[2]

                double grandTotalCatch = 0.0d
                double grandWeightedQSum = 0.0d

                // Суммируем уловы и уловистости по ВСЕМ датам года
                for (int t = 0; t < numDates; t++) {
                    for (int aIdx = 0; aIdx < numAges; aIdx++) {
                        double age = aIdx + 1
                        double c = catchesData[t][aIdx]

                        grandTotalCatch += c
                        grandWeightedQSum += c * sigmoidQ(age, qMax, a50, k)
                    }
                }

                if (grandTotalCatch == 0) return 0.0d

                // Среднегодовой рассчитанный q
                double qCalculatedAnnual = grandWeightedQSum / grandTotalCatch

                // Квадрат отклонения от ЕДИНОГО qKnown
                double error = qCalculatedAnnual - qKnown

                return error * error
            }
        }

        // Оптимизатор BOBYQA
        BOBYQAOptimizer optimizer = new BOBYQAOptimizer(7)

        PointValuePair result = optimizer.optimize(
                new MaxEval(10000),
                new ObjectiveFunction(lossFunction),
                GoalType.MINIMIZE,
                new InitialGuess(initialGuess),
                new SimpleBounds(lowerBounds, upperBounds)
        )

        double[] opt = result.getPoint()
        double qMaxOpt = opt[0]
        double a50Opt  = opt[1]
        double kOpt    = opt[2]

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