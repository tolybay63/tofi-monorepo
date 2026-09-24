package fish.monitoring.test;

import jandcode.core.apx.test.Apx_Test;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;

public class CatchabilityOptimizerRobust extends Apx_Test {


    // 1. Формула сигмоиды
    public static double sigmoidQ(double age, double qMax, double a50, double k) {
        return qMax / (1.0 + Math.exp(-k * (age - a50)));
    }

    public static void main(String[] args) {
        // Исходные данные: уловы по датам (строки) и возрастам (колонки)
        double[][] catchesData = {
                {30,  120, 450, 600, 350, 200}, // Дата 1   Prop_NumberFishCaught 1049
                {15,   80, 310, 520, 410, 180}, // Дата 2
                {50,  200, 500, 480, 300, 150}, // Дата 3
                {100, 300, 600, 400, 200,  50}, // Дата 4
                {10,   50, 200, 450, 500, 300}  // Дата 5
        };

        double[] qKnown = {0.35, 0.35, 0.35, 0.35, 0.35};

        int numDates = catchesData.length;
        int numAges = catchesData[0].length;

        // 2. Целевая функция МНК (Сумма квадратов невязок)
        MultivariateFunction lossFunction = new MultivariateFunction() {
            @Override
            public double value(double[] point) {
                double qMax = point[0];
                double a50  = point[1];
                double k    = point[2];

                double sumSquaredErrors = 0.0;

                for (int t = 0; t < numDates; t++) {
                    double totalCatch = 0.0;
                    double weightedQSum = 0.0;

                    for (int aIdx = 0; aIdx < numAges; aIdx++) {
                        double age = aIdx + 1; // Возраст (1, 2, 3...)
                        double c = catchesData[t][aIdx];

                        totalCatch += c;
                        weightedQSum += c * sigmoidQ(age, qMax, a50, k);
                    }

                    double qCalculatedMean = weightedQSum / totalCatch;
                    double error = qCalculatedMean - qKnown[t];

                    sumSquaredErrors += error * error; // Квадрат невязки
                }

                return sumSquaredErrors; // Мы минимизируем эту сумму
            }
        };

        // 3. Ограничения на параметры (Bounds):
        // qMax: от 0.35 до 1.0
        // a50:  от 0.5 до 10.0 лет
        // k:    от 0.1 до 5.0 (физический запрет на k=0!)
        double[] lowerBounds = {0.35, 0.5, 0.1};
        double[] upperBounds = {1.00, 10.0, 5.0};

        // Начальное приближение [qMax, a50, k]
        double[] initialGuess = {0.60, 3.0, 1.0};

        // 4. Оптимизатор BOBYQA (безпроизводный МНК с ограничениями)
        // Количество точек адаптивной сетки = 2 * N + 1 = 2*3 + 1 = 7
        BOBYQAOptimizer optimizer = new BOBYQAOptimizer(7);

        PointValuePair result = optimizer.optimize(
                new MaxEval(10000),
                new ObjectiveFunction(lossFunction),
                GoalType.MINIMIZE,
                new InitialGuess(initialGuess),
                new SimpleBounds(lowerBounds, upperBounds)
        );

        double[] optimalParams = result.getPoint();
        double qMaxOpt = optimalParams[0];
        double a50Opt  = optimalParams[1];
        double kOpt    = optimalParams[2];

        // 5. Вывод результатов
        System.out.println("=== Успешный МНК с ограничениями (Java BOBYQA) ===");
        System.out.printf("Минимальная ошибка МНК: %.6f%n", result.getValue());
        System.out.printf("1. q_max (макс. уловистость): %.4f%n", qMaxOpt);
        System.out.printf("2. a50 (возраст 50%% отлова): %.3f лет%n", a50Opt);
        System.out.printf("3. k (крутизна сигмоиды):   %.3f%n", kOpt);

        System.out.println("\nРассчитанные точечные коэффициенты q(a) по возрастам:");
        for (int age = 1; age <= numAges; age++) {
            double qA = sigmoidQ(age, qMaxOpt, a50Opt, kOpt);
            System.out.printf("Возраст %d лет: q(%d) = %.4f%n", age, age, qA);
        }
    }



}
