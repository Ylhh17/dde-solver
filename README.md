<div align="center">

# DDE Solver

**A desktop application for solving and visualizing delay differential equations**

[![Java CI](https://github.com/Ylhh17/dde-solver/actions/workflows/java-ci.yml/badge.svg?branch=master)](https://github.com/Ylhh17/dde-solver/actions/workflows/java-ci.yml)
[![Release](https://img.shields.io/github/v/release/Ylhh17/dde-solver?display_name=tag)](https://github.com/Ylhh17/dde-solver/releases/latest)
[![License](https://img.shields.io/github/license/Ylhh17/dde-solver)](LICENSE)
[![Java](https://img.shields.io/badge/Java-24-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/24/)

[English](README.md) · [Русский](README_RU.md)

[**Download for Windows**](https://github.com/Ylhh17/dde-solver/releases/latest) · [Features](#features) · [Build from source](#build-from-source) · [Limitations](#limitations)

</div>

DDE Solver is an educational desktop tool for numerical experiments with **delay differential equations (DDEs)**. It provides three numerical methods, interactive charts, comparison with an exact solution, error metrics, data tables, and CSV export.

The ready-to-use Windows package includes its own Java runtime. No separate Java or Maven installation is required.

> The current application interface is in Russian. English interface localization is planned for a future release; this documentation is available in both languages.

## Features

- Euler, fourth-order Runge–Kutta (RK4), and second-order Adams–Bashforth methods.
- Scalar equations with one or two constant delays.
- User-defined history function \(\varphi(t)\) for \(t \le 0\).
- Linear interpolation of delayed values between grid points.
- Side-by-side comparison of all three methods.
- Comparison with a user-defined exact solution.
- Maximum absolute, relative, RMS, and final-point error metrics.
- Four built-in demonstration problems.
- Tables of computed, delayed, and error values.
- CSV export for one method or all methods.
- Light and dark interface themes.
- Input validation with clear error messages.
- Automated tests, continuous integration, and a portable Windows build.

## Download

1. Open the [latest release](https://github.com/Ylhh17/dde-solver/releases/latest).
2. Download `DDE-Solver-1.0.0-windows-x64.zip`.
3. Extract the complete archive.
4. Run `DDE Solver.exe` inside the extracted `DDE Solver` directory.

Keep the `app` and `runtime` directories next to the executable. The application image is portable and includes the required Java runtime.

Windows may display a SmartScreen warning because the application is not code-signed. Review the downloaded file and choose **More info → Run anyway** only if it came from this repository's official release page.

## Mathematical model

The application solves initial-history problems of the form

\[
y'(t) = f\!\left(t, y(t), y(t-\tau_1), y(t-\tau_2)\right), \quad t \ge 0,
\]

\[
y(t) = \varphi(t), \quad t \le 0.
\]

The user interface currently provides these right-hand-side templates:

\[
y'(t)=a\,y(t-\tau),
\]

\[
y'(t)=a\,y(t-\tau)+b\,y(t),
\]

\[
y'(t)=a\,y(t-\tau_1)+b\,y(t-\tau_2).
\]

Expressions for the history and exact solution are entered using exp4j syntax, for example `exp(0.4*t)`.

## Quick example

After launching the application:

1. Select **«Точная: одна задержка»** from the example list.
2. Choose **«Рунге–Кутты 4»**.
3. Click **«Сравнить с точным»** to compare the numerical and exact solutions.
4. Open **«Таблица погрешностей»** to compare all three methods.
5. Export the results to CSV if needed.

## Demo problems

| Preset | Model | Exact solution |
|---|---|---|
| Basic | One delayed term with decaying dynamics | Not provided |
| Exact: one delay | \(y'(t)=a\,y(t-\tau)\) | `exp(0.4*t)` |
| Exact: delay and state | Delayed and current-state terms | `exp(0.3*t)` |
| Exact: two delays | Two delayed terms | `exp(0.3*t)` |

## Build from source

### Requirements

- JDK 24.
- Windows, Linux, or macOS.
- Git is optional if you download the source archive.

Maven, the JavaFX SDK, and exp4j do not need to be installed separately. Maven Wrapper downloads the pinned Maven version, and Maven resolves the project dependencies.

### Windows

```powershell
git clone https://github.com/Ylhh17/dde-solver.git
cd dde-solver
.\mvnw.cmd clean javafx:run
```

### Linux and macOS

```bash
git clone https://github.com/Ylhh17/dde-solver.git
cd dde-solver
./mvnw clean javafx:run
```

If the wrapper is not executable after extracting an archive:

```bash
chmod +x mvnw
```

## Tests

Run the complete automated test suite:

```powershell
# Windows
.\mvnw.cmd clean test
```

```bash
# Linux and macOS
./mvnw clean test
```

The test suite covers the numerical core, history evaluation, interval boundaries, interpolation, parameter validation, analytical reference problems with one and two delays, and observed convergence orders.

## Windows package

A portable Windows application image can be built locally with a full JDK 24 installation containing `jpackage`:

```powershell
.\scripts\package-windows.ps1
```

The archive is written to:

```text
target/distribution/DDE-Solver-1.0.0-windows-x64.zip
```

## Technology

- Java 24 and JavaFX 24.
- Maven and Maven Wrapper.
- exp4j for mathematical expressions.
- JUnit Jupiter for automated testing.
- GitHub Actions for CI and Windows packaging.
- `jpackage` for the self-contained Windows application image.

## Limitations

- Only scalar DDEs are supported.
- Equations may contain one or two positive constant delays.
- The fixed grid step must not exceed any active delay.
- Delayed values are obtained using linear interpolation.
- The `|Euler − RK4|` chart shows disagreement between two approximations, not a rigorous error estimate.
- The software is intended for education and exploratory numerical experiments, not safety-critical, clinical, or production engineering calculations.

## Roadmap

Potential future improvements include:

- English and Russian application localization.
- Saving and loading experiment files.
- Exporting charts as PNG images.
- A gallery of applied DDE models.
- Automated step-refinement and convergence studies.
- Adaptive step-size control.
- Systems of delay differential equations.

See the [issue tracker](https://github.com/Ylhh17/dde-solver/issues) for planned work and bug reports.

## Development

- `master` contains stable releases.
- `develop` contains ongoing development.
- Pull requests must pass the automated test workflow before merging.

Version history is available in [CHANGELOG.md](CHANGELOG.md).

## License

DDE Solver is distributed under the [MIT License](LICENSE).
