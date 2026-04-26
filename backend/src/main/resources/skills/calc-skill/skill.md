---
name: calc-skill
description: 执行加减乘除数学计算
---

# Calc Skill
Use for addition, subtraction, multiplication, division.

This skill provides basic arithmetic calculation capabilities.
It can compute two numbers with four operators: `+`, `-`, `*`, `/`.

## When to use
Invoke this skill whenever the user asks for math calculation, arithmetic solving or number computing.

## Parameters
- num1: first operand, number
- num2: second operand, number
- op: calculate operator, support + - * /

## Rule
- Support integer and decimal.
- Do not divide by zero, return error tip if divisor is zero.
- Return complete expression and final result.