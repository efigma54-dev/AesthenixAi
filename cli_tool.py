#!/usr/bin/env python3
"""
AESTHENIX AI Code Analyzer — CLI Tool

Professional-grade command-line interface for code analysis.

Features:
  - analyze <path>              Analyze file or directory
  - history <file>              Show analysis trends
  - config                      Display/manage configuration
  - metrics                     Show system metrics
  - compare <v1> <v2>          Compare two versions
  - export <format>            Export analysis results

Interview impact:
  "I built a production CLI tool with progress tracking,
   colored output, and advanced reporting."

Usage:
  aesthenix analyze src/
  aesthenix history src/Example.java --days 30
  aesthenix compare HEAD~1 HEAD
"""

import argparse
import json
import os
import requests
import sys
import time
from pathlib import Path
from datetime import datetime, timedelta
from typing import Optional, List, Dict
from dataclasses import dataclass
from enum import Enum

# Configuration
BASE_URL = os.getenv("AESTHENIX_URL", "http://localhost:8080/api")
COLORS_ENABLED = os.getenv("NO_COLOR") is None and (sys.stdout.isatty())

# ANSI colors for terminal output
class Color:
    RESET = "\033[0m"
    RED = "\033[91m"
    GREEN = "\033[92m"
    YELLOW = "\033[93m"
    BLUE = "\033[94m"
    GRAY = "\033[90m"
    BOLD = "\033[1m"

    @staticmethod
    def disable():
        for attr in dir(Color):
            if not attr.startswith("_"):
                setattr(Color, attr, "")

if not COLORS_ENABLED:
    Color.disable()

@dataclass
class AnalysisResult:
    """Represents a single file analysis result."""
    file: str
    score: float
    total_issues: int
    critical: int
    high: int
    medium: int
    low: int
    suggestions: int
    time_ms: int

    @property
    def status_icon(self) -> str:
        if self.score >= 75:
            return f"{Color.GREEN}✓{Color.RESET}"
        elif self.score >= 50:
            return f"{Color.YELLOW}⚠{Color.RESET}"
        else:
            return f"{Color.RED}✗{Color.RESET}"

    def __str__(self) -> str:
        score_color = Color.GREEN if self.score >= 75 else Color.YELLOW if self.score >= 50 else Color.RED
        return (
            f"{self.status_icon} {self.file:<40} {score_color}{self.score:5.1f}/100{Color.RESET} "
            f"issues:{self.total_issues} (🔴{self.critical} 🟠{self.high} 🟡{self.medium})"
        )


class CLICommand:
    """Base class for CLI subcommands."""
    
    def __init__(self, args):
        self.args = args
        self.api_url = BASE_URL

    def run(self) -> int:
        raise NotImplementedError

    def _get(self, endpoint: str) -> dict:
        """Make GET request to backend API."""
        try:
            response = requests.get(f"{self.api_url}{endpoint}", timeout=30)
            response.raise_for_status()
            return response.json()
        except requests.RequestException as e:
            print(f"{Color.RED}Error: {e}{Color.RESET}", file=sys.stderr)
            return {}

    def _post(self, endpoint: str, data: dict) -> dict:
        """Make POST request to backend API."""
        try:
            response = requests.post(f"{self.api_url}{endpoint}", json=data, timeout=30)
            response.raise_for_status()
            return response.json()
        except requests.RequestException as e:
            print(f"{Color.RED}Error: {e}{Color.RESET}", file=sys.stderr)
            return {}


class AnalyzeCommand(CLICommand):
    """Analyze file or directory."""

    def run(self) -> int:
        path = Path(self.args.path)
        
        if not path.exists():
            print(f"{Color.RED}Error: Path not found: {path}{Color.RESET}", file=sys.stderr)
            return 1

        results = []
        if path.is_file():
            results = [self._analyze_file(path)]
        else:
            results = self._analyze_directory(path)

        self._print_summary(results)
        return 0 if all(r.score >= 50 for r in results) else 1

    def _analyze_file(self, filepath: Path) -> AnalysisResult:
        """Analyze a single file."""
        if not filepath.suffix == ".java":
            print(f"{Color.GRAY}Skipping {filepath} (not Java){Color.RESET}")
            return None

        try:
            with open(filepath, "r") as f:
                code = f.read()

            print(f"{Color.BLUE}Analyzing {filepath}...{Color.RESET}")
            start_ms = int(time.time() * 1000)
            result = self._post("/review", {"code": code})
            elapsed_ms = int(time.time() * 1000) - start_ms

            if not result:
                return None

            return AnalysisResult(
                file=str(filepath),
                score=result.get("score", 0),
                total_issues=len(result.get("issues", [])),
                critical=sum(1 for i in result.get("issues", []) if i.get("severity", 0) >= 8),
                high=sum(1 for i in result.get("issues", []) if 5 <= i.get("severity", 0) < 8),
                medium=sum(1 for i in result.get("issues", []) if 3 <= i.get("severity", 0) < 5),
                low=sum(1 for i in result.get("issues", []) if i.get("severity", 0) < 3),
                suggestions=len(result.get("suggestions", [])),
                time_ms=elapsed_ms
            )

        except Exception as e:
            print(f"{Color.RED}Error analyzing {filepath}: {e}{Color.RESET}", file=sys.stderr)
            return None

    def _analyze_directory(self, dirpath: Path) -> List[AnalysisResult]:
        """Analyze all Java files in directory."""
        java_files = sorted(dirpath.glob("**/*.java"))
        
        if not java_files:
            print(f"{Color.YELLOW}No Java files found in {dirpath}{Color.RESET}")
            return []

        results = []
        for i, filepath in enumerate(java_files, 1):
            print(f"\n[{i}/{len(java_files)}]", end=" ")
            result = self._analyze_file(filepath)
            if result:
                results.append(result)

        return results

    def _print_summary(self, results: List[AnalysisResult]):
        """Print formatted summary."""
        if not results:
            print(f"{Color.GRAY}No results to display{Color.RESET}")
            return

        print(f"\n{Color.BOLD}{'='*80}")
        print(f"ANALYSIS COMPLETE{Color.RESET}")
        print(f"{'='*80}\n")

        for result in results:
            print(result)

        # Aggregate stats
        avg_score = sum(r.score for r in results) / len(results) if results else 0
        total_issues = sum(r.total_issues for r in results)
        total_critical = sum(r.critical for r in results)
        total_high = sum(r.high for r in results)
        total_time_ms = sum(r.time_ms for r in results)

        print(f"\n{Color.BOLD}Summary:{Color.RESET}")
        print(f"  Files analyzed:     {len(results)}")
        print(f"  Average score:      {Color.BLUE}{avg_score:.1f}/100{Color.RESET}")
        print(f"  Total issues:       {total_issues} (🔴{total_critical} 🟠{total_high})")
        print(f"  Total time:         {total_time_ms:,}ms")


class HistoryCommand(CLICommand):
    """Show analysis history and trends."""

    def run(self) -> int:
        filepath = self.args.file
        days = getattr(self.args, "days", 30)

        result = self._get(f"/history/{filepath}?days={days}")
        if not result:
            print(f"{Color.YELLOW}No history found for {filepath}{Color.RESET}")
            return 0

        # Display trend
        trend = result.get("trend", "STABLE")
        score_change = result.get("scoreChange", 0)
        issue_change = result.get("issueChange", 0)

        print(f"\n{Color.BOLD}Trend Analysis: {filepath} (last {days} days){Color.RESET}")
        print(f"  Trend:           {self._trend_icon(trend)} {trend}")
        print(f"  Score change:    {score_change:+.1f}")
        print(f"  Issue change:    {issue_change:+d}")

        return 0

    @staticmethod
    def _trend_icon(trend: str) -> str:
        icons = {"IMPROVED": f"{Color.GREEN}↑", "REGRESSED": f"{Color.RED}↓", "STABLE": f"{Color.GRAY}→"}
        return icons.get(trend, "") + Color.RESET


class ExportCommand(CLICommand):
    """Export analysis results to various formats."""

    def run(self) -> int:
        format_type = self.args.format.lower()
        filepath = self.args.file

        # Get analysis result
        with open(filepath, "r") as f:
            code = f.read()

        result = self._post("/review", {"code": code})
        if not result:
            print(f"{Color.RED}Failed to analyze {filepath}{Color.RESET}", file=sys.stderr)
            return 1

        # Export
        if format_type == "json":
            output = json.dumps(result, indent=2)
        elif format_type == "csv":
            output = self._to_csv(result)
        elif format_type == "html":
            output = self._to_html(result)
        else:
            print(f"{Color.RED}Unknown format: {format_type}{Color.RESET}", file=sys.stderr)
            return 1

        output_file = f"{filepath}.{format_type}"
        with open(output_file, "w") as f:
            f.write(output)

        print(f"{Color.GREEN}✓ Exported to {output_file}{Color.RESET}")
        return 0

    @staticmethod
    def _to_csv(result: dict) -> str:
        """Convert to CSV format."""
        lines = ["type,severity,message,line"]
        for issue in result.get("issues", []):
            lines.append(f"{issue.get('type','')},{issue.get('severity',0)},\"{issue.get('message','')}\",{issue.get('line',0)}")
        return "\n".join(lines)

    @staticmethod
    def _to_html(result: dict) -> str:
        """Convert to HTML format."""
        score = result.get("score", 0)
        score_color = "green" if score >= 75 else "orange" if score >= 50 else "red"
        
        html_parts = [
            f"<html><head><title>Code Review Report</title></head><body>",
            f"<h1>Code Review Report</h1>",
            f"<p><strong>Score:</strong> <span style='color:{score_color}'>{score:.1f}/100</span></p>",
            f"<h2>Issues ({len(result.get('issues', []))})</h2>",
            f"<ul>"
        ]

        for issue in result.get("issues", []):
            html_parts.append(
                f"<li><strong>{issue.get('type', '')}</strong> (line {issue.get('line', 0)}): "
                f"{issue.get('message', '')}</li>"
            )

        html_parts.extend(["</ul></body></html>"])
        return "\n".join(html_parts)


class MetricsCommand(CLICommand):
    """Display system metrics."""

    def run(self) -> int:
        result = self._get("/metrics")
        
        print(f"{Color.BOLD}AI Service Metrics:{Color.RESET}")
        print(f"  Success rate:   {result.get('ai_success_rate', 0):.1f}%")
        print(f"  Avg latency:    {result.get('ai_avg_time_ms', 0):.0f}ms")
        print(f"  Cache size:     {result.get('cache_size', 0)} items")

        return 0


def create_parser() -> argparse.ArgumentParser:
    """Create argument parser for CLI commands."""
    parser = argparse.ArgumentParser(
        description="AESTHENIX AI Code Analyzer — professional code quality tool",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  aesthenix analyze src/                          # Analyze entire directory
  aesthenix analyze Main.java                     # Analyze single file
  aesthenix history src/Example.java --days 30   # Show improvement trend
  aesthenix export file.java --format json       # Export to JSON
  aesthenix metrics                               # Show AI service metrics
        """
    )

    # Global options
    parser.add_argument("--url", default=BASE_URL,
                       help=f"Backend API URL (default: {BASE_URL})")
    parser.add_argument("--no-color", action="store_true",
                       help="Disable colored output")

    subparsers = parser.add_subparsers(dest="command", help="Command to run")

    # analyze command
    analyze_parser = subparsers.add_parser("analyze", help="Analyze file or directory")
    analyze_parser.add_argument("path", help="File or directory to analyze")
    analyze_parser.add_argument("--output", "-o", help="Output file (optional)")

    # history command
    history_parser = subparsers.add_parser("history", help="Show analysis history")
    history_parser.add_argument("file", help="File path")
    history_parser.add_argument("--days", type=int, default=30,
                                help="Number of days of history (default: 30)")

    # export command
    export_parser = subparsers.add_parser("export", help="Export results")
    export_parser.add_argument("file", help="File to export")
    export_parser.add_argument("--format", choices=["json", "csv", "html"],
                              default="json", help="Export format")

    # metrics command
    subparsers.add_parser("metrics", help="Show system metrics")

    return parser


def main() -> int:
    """Main CLI entry point."""
    parser = create_parser()
    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        return 1

    # Update global URL if provided
    global BASE_URL
    if args.url:
        BASE_URL = args.url

    # Disable colors if requested
    if args.no_color:
        Color.disable()

    # Route to command handler
    command_map = {
        "analyze": AnalyzeCommand,
        "history": HistoryCommand,
        "export": ExportCommand,
        "metrics": MetricsCommand,
    }

    command_class = command_map.get(args.command)
    if not command_class:
        print(f"{Color.RED}Unknown command: {args.command}{Color.RESET}", file=sys.stderr)
        return 1

    try:
        command = command_class(args)
        return command.run()
    except Exception as e:
        print(f"{Color.RED}Error: {e}{Color.RESET}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
        output += f"\n💡 Suggestions:\n"
        for suggestion in suggestions[:3]:
            output += f"  • {suggestion}\n"

    return output

def main():
    parser = argparse.ArgumentParser(
        description="AI Code Reviewer CLI Tool",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  aesthenix analyze MyFile.java
  aesthenix analyze-dir ./src
  aesthenix scan-repo https://github.com/user/repo
  aesthenix scan-repo https://github.com/user/repo --token YOUR_GITHUB_TOKEN
        """
    )

    parser.add_argument('--server', default=BASE_URL, help='Server URL (default: http://localhost:8080/api)')
    parser.add_argument('-v', '--verbose', action='store_true', help='Verbose output')

    subparsers = parser.add_subparsers(dest='command', help='Commands')

    # analyze command
    analyze_parser = subparsers.add_parser('analyze', help='Analyze a single Java file')
    analyze_parser.add_argument('file', help='Java file to analyze')

    # analyze-dir command
    dir_parser = subparsers.add_parser('analyze-dir', help='Analyze all Java files in a directory')
    dir_parser.add_argument('directory', help='Directory containing Java files')

    # scan-repo command
    repo_parser = subparsers.add_parser('scan-repo', help='Scan a GitHub repository')
    repo_parser.add_argument('url', help='GitHub repository URL')
    repo_parser.add_argument('--token', help='GitHub personal access token')

    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        sys.exit(0)

    if args.command == 'analyze':
        result = analyze_file(args.file, args.server)
        print(format_output(result, args.verbose))
        print(f"\nFull JSON: {json.dumps(result, indent=2)}")

    elif args.command == 'analyze-dir':
        results = analyze_directory(args.directory, args.server)
        avg_score = sum(r.get('score', 0) for r in results) / len(results) if results else 0
        print(f"\n📊 Directory Analysis Results")
        print(f"   Files analyzed: {len(results)}")
        print(f"   Average score: {avg_score:.1f}/100")
        for result in results:
            print(format_output(result, args.verbose))

    elif args.command == 'scan-repo':
        result = scan_repo(args.url, args.token, args.server)
        print(format_output(result, args.verbose))
        print(f"\nFull JSON: {json.dumps(result, indent=2)}")

if __name__ == '__main__':
    main()