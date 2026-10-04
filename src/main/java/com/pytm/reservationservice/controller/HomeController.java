package com.pytm.reservationservice.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>SeatSync | Reservation Service</title>
                    <style>
                        * {
                            box-sizing: border-box;
                            margin: 0;
                            padding: 0;
                        }

                        body {
                            font-family: Inter, -apple-system, BlinkMacSystemFont,
                                         "Segoe UI", sans-serif;
                            background: #0b1020;
                            color: #f8fafc;
                            min-height: 100vh;
                        }

                        .container {
                            max-width: 1000px;
                            margin: auto;
                            padding: 60px 24px;
                        }

                        .hero {
                            text-align: center;
                            margin-bottom: 45px;
                        }

                        .badge {
                            display: inline-block;
                            padding: 7px 14px;
                            border: 1px solid #334155;
                            border-radius: 999px;
                            color: #93c5fd;
                            background: #111827;
                            font-size: 13px;
                            margin-bottom: 20px;
                        }

                        h1 {
                            font-size: clamp(42px, 7vw, 72px);
                            letter-spacing: -3px;
                            margin-bottom: 15px;
                        }

                        .gradient {
                            background: linear-gradient(90deg, #60a5fa, #a78bfa);
                            -webkit-background-clip: text;
                            -webkit-text-fill-color: transparent;
                        }

                        .subtitle {
                            color: #94a3b8;
                            font-size: 18px;
                            max-width: 650px;
                            margin: auto;
                            line-height: 1.6;
                        }

                        .status {
                            margin: 30px auto;
                            padding: 14px 20px;
                            width: fit-content;
                            border-radius: 12px;
                            background: #052e1b;
                            border: 1px solid #166534;
                            color: #86efac;
                            font-weight: 600;
                        }

                        .grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                            gap: 16px;
                            margin-bottom: 40px;
                        }

                        .card {
                            background: #111827;
                            border: 1px solid #1e293b;
                            border-radius: 16px;
                            padding: 24px;
                            transition: transform 0.2s, border-color 0.2s;
                        }

                        .card:hover {
                            transform: translateY(-4px);
                            border-color: #475569;
                        }

                        .icon {
                            font-size: 25px;
                            margin-bottom: 12px;
                        }

                        .card h3 {
                            margin-bottom: 8px;
                        }

                        .card p {
                            color: #94a3b8;
                            font-size: 14px;
                            line-height: 1.5;
                        }

                        .links {
                            display: flex;
                            flex-wrap: wrap;
                            justify-content: center;
                            gap: 12px;
                            margin-bottom: 45px;
                        }

                        .button {
                            text-decoration: none;
                            color: white;
                            background: #2563eb;
                            padding: 12px 18px;
                            border-radius: 10px;
                            font-weight: 600;
                            font-size: 14px;
                            transition: background 0.2s;
                        }

                        .button:hover {
                            background: #1d4ed8;
                        }

                        .button.secondary {
                            background: #1e293b;
                        }

                        .button.secondary:hover {
                            background: #334155;
                        }

                        footer {
                            text-align: center;
                            color: #64748b;
                            font-size: 13px;
                            padding-top: 20px;
                            border-top: 1px solid #1e293b;
                        }

                        .tech {
                            margin-top: 12px;
                            color: #475569;
                        }
                    </style>
                </head>

                <body>
                    <div class="container">

                        <section class="hero">
                            <div class="badge">● Production API · Online</div>

                            <h1>
                                <span class="gradient">SeatSync</span>
                            </h1>

                            <p class="subtitle">
                                A production-oriented ticket reservation service
                                built with Spring Boot, PostgreSQL and concurrency-safe
                                reservation workflows.
                            </p>

                            <div class="status">
                                🟢 Service is running
                            </div>
                        </section>

                        <div class="links">
                            <a class="button"
                               href="/swagger-ui.html">
                                📖 Swagger UI
                            </a>

                            <a class="button secondary"
                               href="/v3/api-docs">
                                OpenAPI
                            </a>

                            <a class="button secondary"
                               href="/actuator/health">
                                Health
                            </a>

                            <a class="button secondary"
                               href="/actuator/prometheus">
                                Metrics
                            </a>
                        </div>

                        <div class="grid">

                            <div class="card">
                                <div class="icon">🔒</div>
                                <h3>Concurrency Safe</h3>
                                <p>
                                    Database row locking prevents multiple users
                                    from reserving the same seat concurrently.
                                </p>
                            </div>

                            <div class="card">
                                <div class="icon">♻️</div>
                                <h3>Idempotent</h3>
                                <p>
                                    Duplicate requests can safely be retried
                                    using an idempotency key.
                                </p>
                            </div>

                            <div class="card">
                                <div class="icon">👤</div>
                                <h3>User Limits</h3>
                                <p>
                                    Configurable reservation limits protect
                                    against excessive reservations per user.
                                </p>
                            </div>

                            <div class="card">
                                <div class="icon">📊</div>
                                <h3>Observable</h3>
                                <p>
                                    Actuator, Prometheus metrics and correlation
                                    IDs provide operational visibility.
                                </p>
                            </div>

                            <div class="card">
                                <div class="icon">🗄️</div>
                                <h3>PostgreSQL</h3>
                                <p>
                                    Persistent relational storage with Flyway
                                    database migrations.
                                </p>
                            </div>

                            <div class="card">
                                <div class="icon">🚀</div>
                                <h3>Cloud Deployed</h3>
                                <p>
                                    Containerized Spring Boot application
                                    deployed on Render.
                                </p>
                            </div>

                        </div>

                        <footer>
                            SeatSync Reservation Service
                            <div class="tech">
                                Java 21 · Spring Boot · PostgreSQL · Flyway · Docker · Render
                            </div>
                        </footer>

                    </div>
                </body>
                </html>
                """;
    }
}