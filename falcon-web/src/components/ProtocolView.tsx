import React from 'react'

export const ProtocolView: React.FC = () => {
    return (
        <div className="protocol-container">
            <header className="protocol-header">
                <h1>Project Falcon: An Adversarial Algorithmic Trust Protocol</h1>
                <p className="author">Johanna Almeida | February 2026</p>
                <div className="badge-cloud">
                    <span className="badge">Draft Standard</span>
                    <span className="badge">arXiv:cs.CR</span>
                    <span className="badge">EAS:Verifiable</span>
                </div>
            </header>

            <section className="abstract">
                <h2>Abstract</h2>
                <p>
                    Falcon introduces an automated, zero-trust reputation framework designed for the AT Protocol ecosystem.
                    By utilizing subjective transitive trust with temporal decay and non-linear squashing,
                    Falcon establishes a mathematical standard for decentralized trust that is resistant to Sybil attacks and collusion.
                </p>
            </section>

            <section className="math-preview">
                <h2>Transitive Trust (STT) Score</h2>
                <div className="formula-box">
                    <code>
                        S(u, v) = tanh( Σ [ T(u, w) * T(w, v) * exp(-λΔt) ] / Σ |T(u, w)| )
                    </code>
                </div>
            </section>

            <section className="links">
                <a href="/ProjectFalcon_arXiv_Package.zip" className="download-btn" download>
                    Download arXiv Submission Package (.zip)
                </a>
            </section>

            <style>{`
        .protocol-container {
          max-width: 800px;
          margin: 0 auto;
          padding: 4rem 2rem;
          color: var(--text-primary);
          font-family: 'Inter', sans-serif;
          line-height: 1.6;
        }
        .protocol-header {
          text-align: center;
          margin-bottom: 4rem;
        }
        .protocol-container h1 {
          font-size: 2.5rem;
          font-weight: 800;
          letter-spacing: -0.05em;
          margin-bottom: 1rem;
        }
        .author {
          color: var(--text-secondary);
          font-family: monospace;
          margin-bottom: 2rem;
        }
        .badge-cloud {
          display: flex;
          gap: 0.5rem;
          justify-content: center;
        }
        .badge {
          background: var(--bg-tertiary);
          padding: 0.25rem 0.75rem;
          border-radius: 99px;
          font-size: 0.75rem;
          font-weight: 600;
          color: var(--accent-primary);
          border: 1px solid var(--border-color);
        }
        .abstract {
          background: var(--bg-secondary);
          padding: 2rem;
          border-radius: 12px;
          border: 1px solid var(--border-color);
          margin-bottom: 3rem;
        }
        .protocol-container h2 {
          font-size: 1.25rem;
          font-weight: 700;
          margin-bottom: 1rem;
          text-transform: uppercase;
          letter-spacing: 0.05em;
        }
        .formula-box {
          background: #000;
          color: #0f0;
          padding: 1.5rem;
          border-radius: 8px;
          font-family: 'Fira Code', monospace;
          text-align: center;
          margin: 2rem 0;
          overflow-x: auto;
        }
        .download-btn {
          display: block;
          text-align: center;
          background: var(--accent-primary);
          color: #fff;
          padding: 1rem;
          border-radius: 8px;
          text-decoration: none;
          font-weight: 700;
          transition: transform 0.2s;
        }
        .download-btn:hover {
          transform: translateY(-2px);
          filter: brightness(1.1);
        }
      `}</style>
        </div>
    )
}
