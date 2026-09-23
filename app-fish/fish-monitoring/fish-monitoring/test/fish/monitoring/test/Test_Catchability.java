package fish.monitoring.test;

import jandcode.core.apx.test.Apx_Test;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;


public class Test_Catchability extends Apx_Test {


    public static class CatchabilityOptimizer {

        // 1. Сигмоидная функция q(a)
        public static double sigmoidQ(double age, double qMax, double a50, double k) {
            return qMax / (1.0 + Math.exp(-k * (age - a50)));
        }

        public static void main(String[] args) {
            // 2. Исходные данные
            // Матрица уловов (строки — даты облова, колонки — возрасты 1, 2, 3, 4, 5, 6 лет)
            double[][] catchesData = {
                    {30, 120, 450, 600, 350, 200}, // Дата 1
                    {15, 80, 310, 520, 410, 180}, // Дата 2
                    {50, 200, 500, 480, 300, 150}, // Дата 3
                    {100, 300, 600, 400, 200, 50}, // Дата 4
                    {10, 50, 200, 450, 500, 300}  // Дата 5
            };

            // Известный средний коэффициент уловистости q_known для каждой даты
            double[] qKnown = {0.35, 0.35, 0.35, 0.35, 0.35};

            int numDates = catchesData.length;
            int numAges = catchesData[0].length;

            // 3. Формирование задачи нелинейного МНК
            LeastSquaresProblem problem = new LeastSquaresBuilder()
                    // Начальное приближение параметров: [qMax, a50, k]
                    .start(new double[]{0.5, 3.0, 1.0})
                    // Целевые известные значения y (q_known)
                    .target(qKnown)
                    // Определение функции расхождения и её производных (Якобиана)
                    .model((point) -> {
                        double qMax = point.getEntry(0);
                        double a50 = point.getEntry(1);
                        double k = point.getEntry(2);

                        double[] value = new double[numDates];
                        double[][] jacobian = new double[numDates][3]; // Производные по [qMax, a50, k]

                        for (int t = 0; t < numDates; t++) {
                            double totalCatch = 0.0;
                            double weightedQSum = 0.0;

                            double dQ_dqMax = 0.0;
                            double dQ_da50 = 0.0;
                            double dQ_dk = 0.0;

                            for (int aIdx = 0; aIdx < numAges; aIdx++) {
                                double age = aIdx + 1; // Возраст (1, 2, 3...)
                                double c = catchesData[t][aIdx];
                                totalCatch += c;

                                double expTerm = Math.exp(-k * (age - a50));
                                double denom = 1.0 + expTerm;
                                double qAge = qMax / denom;

                                weightedQSum += c * qAge;

                                // Частные производные сигмоиды для матричной оптимизации
                                dQ_dqMax += c * (1.0 / denom);
                                dQ_da50 += c * (-qMax * expTerm * k / (denom * denom));
                                dQ_dk += c * (qMax * expTerm * (age - a50) / (denom * denom));
                            }

                            // Модельный средний q_t для даты t
                            value[t] = weightedQSum / totalCatch;

                            // Заполнение матрицы Якобиана
                            jacobian[t][0] = dQ_dqMax / totalCatch;
                            jacobian[t][1] = dQ_da50 / totalCatch;
                            jacobian[t][2] = dQ_dk / totalCatch;
                        }

                        RealVector valVector = new ArrayRealVector(value);
                        RealMatrix jacMatrix = new Array2DRowRealMatrix(jacobian);

                        return new Pair<>(valVector, jacMatrix);
                    })
                    .maxEvaluations(1000)
                    .maxIterations(1000)
                    .build();

            // 4. Запуск оптимизатора Левенберга-Маркварда
            LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
            LeastSquaresOptimizer.Optimum optimum = optimizer.optimize(problem);

            double[] optimalParams = optimum.getPoint().toArray();
            double qMaxOpt = optimalParams[0];
            double a50Opt = optimalParams[1];
            double kOpt = optimalParams[2];

            // 5. Вывод результатов
            System.out.println("=== Результаты МНК в Java (Apache Commons Math) ===");
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


}
