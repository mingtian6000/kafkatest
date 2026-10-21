```
The current selected programming language is Java. We emphasize the submission of a fully working code over partially correct but efficient code. Once submitted, you cannot review this problem again. You can use System.out.println() to debug your code. The System.out.println() may not work in case of syntax/runtime error. The version of JDK being used is Java 8.

Note: The main class name must be "Solution".

An employee in an organization has begun working on N projects (numbered 0 to N-1). Each week he/she can work on a single module of one of the projects. The modules that are chosen on any two successive weeks should come from different projects. A project i can have at most Ci modules. The modules of the projects are such that a module is completed in a week.

Write an algorithm to determine the number of weeks the employee can work on projects following the above-mentioned rules.

Input

The first line of the input consists of an integer - num, representing the number of projects (N).

The next line consists of N space-separated integers - projC0, projC1,..., projCN-1, representing the number of modules of the projects.

Output

Print an integer representing the maximum number of weeks the employee can work on the projects.

Constraints

1 ≤ num ≤ 5*10^4

1 ≤ projCi ≤ 10^7

Σ projCi ≤ 10^5

0 ≤ i < num

Example

Input:

3

7 2 3

Output:

11

Explanation:

The first, second and third projects have 7, 2 and 3 modules respectively.

The modules of different projects are selected on successive weeks in a sequence: first, second, first, third, first, second, first, third, first, third, first.