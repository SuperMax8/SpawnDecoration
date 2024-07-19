package fr.supermax_8.spawndecoration.utils;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

import java.util.Map;
import java.util.HashMap;
import java.util.Random;

public class EquationParser {
    private Map<String, Expression> expressions;
    private Random random;

    public EquationParser() {
        expressions = new HashMap<>();
        random = new Random();
    }

    public void addEquation(String fn, String equation) {
        // Ajouter la fonction personnalisée rand(min, max)
        Function randFunction = new Function("rand", 2) {
            @Override
            public double apply(double... args) {
                double min = args[0];
                double max = args[1];
                return min + (max - min) * random.nextDouble();
            }
        };

        Function minFunction = new Function("min", 2) {
            @Override
            public double apply(double... args) {
                return Math.min(args[0], args[1]);
            }
        };

        Function maxFunction = new Function("max", 2) {
            @Override
            public double apply(double... args) {
                return Math.max(args[0], args[1]);
            }
        };

        Expression expression = new ExpressionBuilder(equation)
                .variables("t", "i")
                .functions(randFunction, minFunction, maxFunction)
                .build();
        expressions.put(fn, expression);
    }

    public double evaluate(String fn, double t, double i) {
        if (expressions.containsKey(fn)) {
            Expression expression = expressions.get(fn);
            return expression.setVariable("t", t).setVariable("i", i).evaluate();
        }
        return 0;
    }

}